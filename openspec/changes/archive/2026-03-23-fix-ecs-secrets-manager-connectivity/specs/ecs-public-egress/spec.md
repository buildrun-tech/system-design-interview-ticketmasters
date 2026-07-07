## ADDED Requirements

### Requirement: ECS tasks receive public IPs in IGW-routed subnets
The system SHALL configure the ECS service to assign public IP addresses to task ENIs when tasks run in subnets whose route table sends `0.0.0.0/0` to an Internet Gateway.

#### Scenario: ECS service enables public IP assignment
- **WHEN** the ECS service network configuration is rendered
- **THEN** `assign_public_ip` is set to `true`

#### Scenario: Tasks can use the public route table for outbound traffic
- **WHEN** ECS launches tasks in subnets associated with a route table that sends `0.0.0.0/0` to an Internet Gateway
- **THEN** the task ENIs receive public IPs and can initiate outbound internet connections

### Requirement: ECS secret bootstrap uses public AWS endpoints
The system SHALL allow ECS tasks that use `valueFrom = var.db_password_secret_arn` in the task definition to retrieve the secret during task initialization through the public Secrets Manager endpoint.

#### Scenario: Task startup can fetch the database password secret
- **WHEN** ECS launches a task revision that references `var.db_password_secret_arn`
- **THEN** the task reaches startup without a Secrets Manager connection timeout caused by missing public internet reachability

#### Scenario: Existing secret reference contract remains unchanged
- **WHEN** the task definition is rendered
- **THEN** `QUARKUS_DATASOURCE_PASSWORD` continues to be sourced from `var.db_password_secret_arn`
