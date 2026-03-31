## ADDED Requirements

### Requirement: ECS service adopts the latest Terraform-managed task definition revision
The system SHALL update the ECS service to the task definition revision produced by the current Terraform deployment when the rendered task definition changes.

#### Scenario: New task definition revision is registered during apply
- **WHEN** Terraform creates a new ECS task definition revision for the application service
- **THEN** the same apply updates the ECS service to reference that new revision

#### Scenario: No stale revision remains active after successful rollout
- **WHEN** the deployment completes successfully after a task definition change
- **THEN** the ECS service does not remain configured to the previously active task definition revision

### Requirement: Deployment verifies ECS service revision synchronization
The system SHALL validate after deployment that the ECS service references the expected task definition revision.

#### Scenario: Service revision matches expected revision
- **WHEN** CI/CD checks the ECS service after Terraform apply
- **THEN** the workflow confirms the service task definition matches the revision created or selected for that deployment

#### Scenario: Service revision remains outdated
- **WHEN** CI/CD checks the ECS service after Terraform apply and finds an older revision still active
- **THEN** the workflow fails with a message that the ECS service did not advance to the expected task definition revision

### Requirement: No-op deployments do not trigger unnecessary ECS rollouts
The system SHALL avoid forcing a new ECS deployment when the task definition revision has not changed.

#### Scenario: Terraform detects no task definition change
- **WHEN** a deployment runs without any rendered change to the ECS task definition
- **THEN** the ECS service is not restarted solely to satisfy the synchronization mechanism
