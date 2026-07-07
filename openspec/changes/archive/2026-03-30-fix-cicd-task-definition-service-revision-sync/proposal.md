## Why

The pipeline can register a new ECS task definition revision, but the ECS service stays pinned to the previous revision because Terraform explicitly ignores `task_definition` changes on the service. This leaves deployments appearing successful while the running service continues using stale containers and configuration.

## What Changes

- Remove the Terraform behavior that suppresses ECS service updates when a new task definition revision is created
- Define the deployment contract so a successful infrastructure rollout updates the ECS service to the latest task definition revision managed by Terraform
- Add pipeline validation and observability so CI/CD can detect when the service did not advance to the expected revision
- Document the expected behavior for task definition registration, service rollout, and rollback troubleshooting

## Capabilities

### New Capabilities
- `ecs-service-task-sync`: Ensure ECS services managed by Terraform adopt the latest task definition revision produced by a deployment

### Modified Capabilities

## Impact

- `terraform/modules/ecs/main.tf`: remove or replace the lifecycle behavior that ignores `task_definition` updates on the ECS service
- `.github/workflows/deploy.yml`: add deployment verification that confirms the ECS service points to the expected task definition revision after apply
- Terraform/ECS runtime behavior: a new task definition revision now triggers a service update instead of remaining only as an inactive revision
- Operator workflow: deployment failures can be diagnosed from explicit revision checks instead of inferring from stale ECS service state
