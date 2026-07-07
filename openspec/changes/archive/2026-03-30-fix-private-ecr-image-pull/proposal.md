## Why

The ECS service fails with `CannotPullContainerError` because the task definition requests `311141562939.dkr.ecr.us-east-2.amazonaws.com/ticketmaster-dev:latest`, but the delivery workflow publishes environment-specific tags such as `latest-dev` and commit SHA tags instead. This creates a false signal that ECR is unreachable when the actual issue is that the requested image tag does not exist.

## What Changes

- Replace the hardcoded `:latest` ECS image reference with a configurable image tag or full image reference that matches the tags published to ECR
- Wire Terraform inputs so each environment can select the correct default image tag without editing module code
- Add deployment validation that checks the expected image tag exists in ECR before ECS rolls out a task definition
- Document the expected contract between the image publishing workflow and the ECS deployment configuration

## Capabilities

### New Capabilities
- `ecs-image-reference`: Ensure ECS task definitions use an image reference that matches a published ECR tag for the target environment

### Modified Capabilities

## Impact

- `terraform/modules/ecs/main.tf`: stop hardcoding `:latest` in the container image field
- `terraform/modules/ecs/variables.tf`: add image tag or image URI inputs used by the task definition
- `terraform/variables.tf` and `terraform/main.tf`: pass environment-specific image settings into the ECS module
- `.github/workflows/deploy.yml`: align deployment inputs and validation with the image tags pushed to ECR
- Runtime impact: ECS deployments pull an existing image tag and fail earlier with clearer validation if the tag is missing
