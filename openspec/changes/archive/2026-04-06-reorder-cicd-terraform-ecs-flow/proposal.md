## Why

The current CI/CD workflow allows Terraform to run before the image built or promoted by the same pipeline execution is available in ECR. Because Terraform owns the ECS task definition and service rollout, this ordering breaks the deployment contract and can leave ECS pointing at a stale or unintended image.

## What Changes

- Reorder the deployment workflow so `dev` runs `build/test -> push image to ECR -> terraform plan/apply -> rollout verification`
- Reorder the deployment workflow so `prod` runs `image promotion -> terraform plan/apply -> rollout verification`
- Keep Terraform as the single owner of ECS task definition and service updates instead of adding a separate imperative ECS deploy step
- Pass the image selected by the current workflow run into Terraform so ECS rolls forward to the artifact built or promoted by that run
- Replace the current deployment placeholder with post-apply verification and status reporting for the Terraform-managed ECS rollout

## Capabilities

### New Capabilities
- `terraform-managed-ecs-rollout`: Define the required CI/CD stage ordering and rollout checks when Terraform is responsible for updating ECS

### Modified Capabilities
- `ecs-image-reference`: The deployment path selects the exact image reference produced earlier in the same pipeline run instead of relying only on static environment defaults

## Impact

- `.github/workflows/deploy.yml`: reorder job dependencies, carry image metadata into Terraform, and verify rollout after apply
- `terraform/variables.tf` and environment tfvars: accept the workflow-selected image tag or reference for the target environment
- `terraform/main.tf` and `terraform/modules/ecs/*`: render the ECS task definition from the workflow-selected image input while preserving Terraform ownership of the ECS service
- Deployment operations: `dev` continues to build and publish images, while `prod` continues to promote an existing image before infrastructure apply
