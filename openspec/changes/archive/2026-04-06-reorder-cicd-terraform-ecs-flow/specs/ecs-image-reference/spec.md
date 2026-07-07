## MODIFIED Requirements

### Requirement: ECS task definition image tag is configurable
The system SHALL allow the ECS task definition to use a configured image tag, including a tag selected by the current workflow run, instead of hardcoding a static tag in module code.

#### Scenario: Task definition composes image from repository and selected tag
- **WHEN** Terraform renders the ECS task definition
- **THEN** the container image reference uses the configured ECR repository URL and the selected image tag for that deployment

#### Scenario: Workflow-selected tag overrides the default
- **WHEN** the deployment workflow provides an explicit image tag for the current run
- **THEN** the ECS task definition uses that provided tag instead of the environment default tag

### Requirement: Environment defaults match published ECR tags
The system SHALL retain environment-specific default image tags that match the tags published by the delivery workflow when no explicit image tag override is provided for the current deployment.

#### Scenario: Dev default is used when no override is provided
- **WHEN** the dev deployment does not provide an explicit image tag
- **THEN** the ECS configuration uses the default tag published for dev images

#### Scenario: Prod default is used when no override is provided
- **WHEN** the prod deployment does not provide an explicit image tag
- **THEN** the ECS configuration uses the default tag published for prod images

### Requirement: Deployment validates image tag availability before ECS rollout
The system SHALL verify that the selected ECR repository and image tag exist before Terraform updates the ECS service.

#### Scenario: Deployment stops when selected tag is missing
- **WHEN** the deployment workflow cannot find the selected image tag in ECR
- **THEN** the workflow fails before Terraform apply updates the ECS service

#### Scenario: Deployment proceeds when selected tag exists
- **WHEN** the deployment workflow confirms the selected image tag exists in ECR
- **THEN** the workflow continues to the Terraform-managed ECS rollout
