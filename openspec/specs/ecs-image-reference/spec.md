# ecs-image-reference Specification

## Purpose
TBD - created by archiving change fix-private-ecr-image-pull. Update Purpose after archive.
## Requirements
### Requirement: ECS task definition image tag is configurable
The system SHALL allow the ECS task definition to use a configured image tag instead of hardcoding `latest`.

#### Scenario: Task definition composes image from repository and configured tag
- **WHEN** Terraform renders the ECS task definition
- **THEN** the container image reference uses the configured ECR repository URL and configured image tag

#### Scenario: Task definition does not assume generic latest tag
- **WHEN** no custom image override logic is applied in the ECS module
- **THEN** the module does not append the literal `latest` tag to the repository URL

### Requirement: Environment defaults match published ECR tags
The system SHALL define environment-specific default image tags that match the tags published by the delivery workflow.

#### Scenario: Dev defaults to the published dev tag
- **WHEN** the dev environment deploys without an explicit image tag override
- **THEN** the ECS configuration uses the default tag published for dev images

#### Scenario: Prod defaults to the published prod tag
- **WHEN** the prod environment deploys without an explicit image tag override
- **THEN** the ECS configuration uses the default tag published for prod images

### Requirement: Deployment validates image tag availability before ECS rollout
The system SHALL verify that the selected ECR repository and image tag exist before updating the ECS service.

#### Scenario: Deployment stops when tag is missing
- **WHEN** the deployment workflow cannot find the configured image tag in ECR
- **THEN** the workflow fails before triggering an ECS rollout

#### Scenario: Deployment proceeds when tag exists
- **WHEN** the deployment workflow confirms the configured image tag exists in ECR
- **THEN** the workflow continues to the ECS deployment step

