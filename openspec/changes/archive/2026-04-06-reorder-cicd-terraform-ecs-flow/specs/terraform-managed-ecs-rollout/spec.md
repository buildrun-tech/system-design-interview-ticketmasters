## ADDED Requirements

### Requirement: Dev deployments complete image publication before Terraform apply
The system SHALL complete application build/test and publish the dev image to ECR before Terraform starts a dev deployment that can update ECS.

#### Scenario: Dev apply waits for build and image push
- **WHEN** the workflow runs for the dev environment
- **THEN** the Terraform deployment stage starts only after build/test and dev image publication succeed

#### Scenario: Dev apply is skipped when image publication fails
- **WHEN** build/test or dev image publication fails
- **THEN** the workflow does not run the Terraform deployment stage for that dev deployment

### Requirement: Prod deployments complete image promotion before Terraform apply
The system SHALL complete prod image promotion before Terraform starts a prod deployment that can update ECS.

#### Scenario: Prod apply waits for image promotion
- **WHEN** the workflow runs for the prod environment
- **THEN** the Terraform deployment stage starts only after prod image promotion succeeds

#### Scenario: Prod apply is skipped when promotion fails
- **WHEN** prod image promotion fails or the source image is unavailable
- **THEN** the workflow does not run the Terraform deployment stage for that prod deployment

### Requirement: Terraform owns ECS application rollout
The system SHALL update the ECS task definition and ECS service through Terraform apply instead of a separate imperative deployment step.

#### Scenario: Apply updates ECS using the selected image
- **WHEN** Terraform applies a deployment with the image reference selected for that run
- **THEN** the ECS task definition is rendered from that image reference and the ECS service rollout occurs through Terraform-managed resources

#### Scenario: No separate ECS deploy step runs after apply
- **WHEN** the workflow enters the post-apply stage
- **THEN** it verifies rollout state and does not issue a second imperative ECS deployment command

### Requirement: Post-apply verification confirms rollout outcome
The system SHALL verify after Terraform apply that ECS references the expected task definition for the current deployment.

#### Scenario: Rollout verification succeeds
- **WHEN** the ECS service matches the expected task definition after Terraform apply
- **THEN** the workflow reports a successful Terraform-managed rollout

#### Scenario: Rollout verification fails
- **WHEN** the ECS service does not match the expected task definition after Terraform apply
- **THEN** the workflow fails with a rollout synchronization error
