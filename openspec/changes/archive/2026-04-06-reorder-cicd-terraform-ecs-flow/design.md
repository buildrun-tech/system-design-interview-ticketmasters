## Context

The current GitHub Actions workflow lets the infrastructure job run immediately after setup, while the `dev` image build and the `prod` image promotion happen in separate parallel branches. At the same time, Terraform owns both the ECS task definition and the ECS service, which means the effective application deployment already happens during `terraform apply` rather than in the later placeholder deploy job.

This creates two gaps in the deployment contract. First, Terraform can apply before the image built or promoted by the same workflow run is available in ECR. Second, the workflow does not hand the image selected by the current run back into Terraform, so ECS can continue to reference a static environment tag instead of the exact artifact produced earlier in the pipeline.

## Goals / Non-Goals

**Goals:**
- Enforce a stage order where Terraform only runs after the required image artifact is available in ECR for the target environment
- Preserve Terraform as the single source of truth for ECS task definition and service updates
- Ensure `dev` uses build-and-push outputs from the current run and `prod` uses image-promotion outputs from the current run
- Keep rollout verification after apply so the workflow proves ECS adopted the expected task definition

**Non-Goals:**
- Introduce a separate imperative ECS deployment step with `aws ecs update-service`
- Redesign the broader release strategy beyond the current `dev` build and `prod` promotion model
- Move this change to digest-only deployments if commit-specific tags are sufficient for the current pipeline contract

## Decisions

### Gate Terraform on image readiness for each environment
The workflow will make the infrastructure job depend on the image-producing stage for the active environment. In `dev`, Terraform waits for `build-and-test` and `docker-build` to complete. In `prod`, Terraform waits for `image-promotion` to complete. This enforces the desired pipeline order without introducing separate workflows.

Alternative considered: keeping the current parallel structure and relying on mutable environment tags such as `latest-dev` and `latest-prod`. This was rejected because Terraform can apply before the artifact exists, and reusing the same mutable tag does not guarantee a task definition change for the current run.

### Pass the workflow-selected image tag into Terraform
The workflow already produces commit-specific tags during `docker-build` and `image-promotion`. The design will reuse those outputs as the deployment input to Terraform, adding a root Terraform variable that is forwarded into the ECS module and rendered into the ECS task definition image reference.

Alternative considered: switching immediately to image digests. Digests are stronger, but passing the existing commit-specific tag is the smaller change because the workflow already emits those values and the rest of the stack is tag-oriented today.

### Keep the post-apply stage as rollout verification, not deployment
The current `deploy` job should stop pretending to be a future deploy phase and instead become a verification/reporting phase that runs after Terraform. The actual ECS rollout remains part of `terraform apply`, while the final stage validates revision sync and reports success or failure.

Alternative considered: adding a dedicated ECS deploy job after Terraform. This was rejected because it splits ownership of the ECS service between Terraform and imperative workflow logic, which would recreate the same drift risk the repository is already trying to remove.

### Align prod promotion with prod Terraform inputs
The `prod` path will continue to promote a validated image instead of rebuilding it, but the promoted tag from that step becomes the tag Terraform uses for the prod task definition. This keeps the release model intact while making the infrastructure apply deploy the promoted artifact instead of whichever static tag the task definition happened to reference previously.

Alternative considered: keeping prod on a static default tag and treating promotion as a side effect. This was rejected because promotion would no longer guarantee that the subsequent Terraform apply references the promoted artifact.

## Risks / Trade-offs

- Longer critical path in CI/CD because infrastructure now waits for image publication -> Mitigation: accept the extra gating because it removes incorrect rollout ordering
- Commit-tag rollout still depends on tag management rather than immutable digests -> Mitigation: use commit-specific tags as the per-run contract now and leave digest promotion as a future hardening step
- Existing Terraform environment defaults may conflict with workflow-supplied image inputs -> Mitigation: define a clear precedence where explicit workflow inputs override defaults for that run
- The current workflow has placeholder and output wiring issues beyond ordering -> Mitigation: scope this change to the minimum set of job dependencies, image handoff, and verification updates needed for a coherent deployment contract

## Migration Plan

1. Update Terraform inputs so the ECS task definition can receive the image tag selected by the workflow run.
2. Reorder workflow dependencies so image publication completes before Terraform plan/apply for the active environment.
3. Make the post-apply stage verify rollout results instead of acting as a deploy placeholder.
4. Validate in `dev` that a new build produces a new image tag, Terraform updates the task definition, and ECS adopts the expected revision.
5. Validate in `prod` that image promotion feeds the prod Terraform apply and rolls out the promoted artifact.
6. Roll back by restoring the prior workflow dependencies and Terraform image input behavior only if the new handoff breaks deployments, noting that this reintroduces stale-image risk.

## Open Questions

- Should the verification stage wait only for the ECS service task definition to match the expected revision, or also for `services-stable` before reporting success?
- After this change lands, should a follow-up move the deployment contract from commit tags to promoted digests?
