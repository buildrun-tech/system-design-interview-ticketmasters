## Context

The current ECS task definition composes its container image as `${var.ecr_repository_url}:latest`, while the delivery workflow publishes `latest-dev`, `latest-prod`, and commit-specific tags. When ECS deploys against `:latest`, ECR returns `not found`, which surfaces as `CannotPullContainerError` during task startup. The fix crosses Terraform inputs, ECS module behavior, and deployment validation, so the image-tag contract needs to be explicit before implementation.

## Goals / Non-Goals

**Goals:**
- Make the ECS image reference configurable so Terraform requests a tag that is actually published to ECR
- Provide environment-appropriate defaults without hardcoding deployment behavior inside the ECS module
- Fail deployments earlier with a clear validation step when the expected tag is missing from ECR
- Document the tag contract between image build/push and ECS deployment

**Non-Goals:**
- Rework the CI/CD pipeline into digest-only deployments
- Change ECR repository layout or AWS account boundaries
- Address unrelated ECS networking or IAM issues

## Decisions

### Use a dedicated image tag input in Terraform
The ECS module will accept a new image tag input and compose the final image reference from `ecr_repository_url` plus that tag. This is the smallest change that removes the bad `:latest` assumption while preserving the existing repository URL interface.

Alternative considered: passing a full image URI into the module. That is more flexible, but it pushes repository/tag parsing responsibility outward and creates a larger configuration change than needed for this failure mode.

### Set environment-specific defaults at the root module boundary
The root Terraform configuration will own the default tag selection so dev can use `latest-dev` and prod can use `latest-prod`, or later override with a release tag. This keeps the ECS module reusable and avoids embedding environment policy inside a shared module.

Alternative considered: inferring the tag from `environment` inside the ECS module. That is convenient short term, but it couples module behavior to one repository tagging convention.

### Validate image existence before ECS deployment
The deployment workflow will verify that the selected repository and tag exist in ECR before updating ECS. This catches configuration drift earlier and produces a direct error message instead of a delayed ECS task failure.

Alternative considered: relying on ECS rollout failure alone. That keeps the workflow simpler, but the resulting error is slower and easier to misdiagnose as network access or permission failure.

## Risks / Trade-offs

- Mutable environment tags can drift from the exact artifact intended for rollout -> Mitigation: keep support for overriding the default with a specific release or commit tag
- Workflow and Terraform defaults can diverge again over time -> Mitigation: centralize the tag names in deployment inputs and validate them in CI before rollout
- Existing operators may expect `latest` to work -> Mitigation: update docs and defaults together so the contract is visible in configuration

## Migration Plan

1. Add Terraform variables for the ECS image tag and wire them through the root module into the ECS module.
2. Update the ECS task definition to use the configured tag instead of `latest`.
3. Align the deployment workflow to pass or validate the same tag names it publishes.
4. Deploy to dev and confirm new ECS tasks pull an existing image tag from ECR.
5. Roll back by restoring the prior Terraform/task-definition image reference behavior if the new tag wiring is incorrect.

## Open Questions

- Should the deployment path eventually promote immutable digests rather than mutable environment tags? Not required for this fix, but it would reduce tag drift risk.
