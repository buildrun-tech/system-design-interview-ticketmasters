## ADDED Requirements

### Requirement: SQS module creates a standard queue

The Terraform `modules/sqs` module SHALL create an AWS SQS standard queue with a configurable name, visibility timeout, and message retention period.

#### Scenario: Queue is created with default settings

- **WHEN** the module is instantiated with only `queue_name`, `name_prefix`, and `common_tags`
- **THEN** a standard SQS queue is created with visibility timeout of 30 seconds and message retention of 4 days

#### Scenario: Queue is created with custom settings

- **WHEN** the module is instantiated with custom `visibility_timeout_seconds` and `message_retention_seconds`
- **THEN** the queue is created with the specified values

### Requirement: SQS module creates a companion DLQ

The module SHALL create a dead-letter queue (DLQ) named `<queue_name>-dlq` and configure a redrive policy on the main queue pointing to it.

#### Scenario: DLQ receives messages after max receive count

- **WHEN** a message is received from the main queue more than `max_receive_count` times without being deleted
- **THEN** the message is moved to the DLQ

#### Scenario: DLQ name follows convention

- **WHEN** the main queue is named `check-booking-pending-state`
- **THEN** the DLQ is named `check-booking-pending-state-dlq`

### Requirement: Module outputs queue URL and ARN

The module SHALL expose `queue_url` and `queue_arn` as outputs so callers can pass them to other resources (e.g., ECS environment variables).

#### Scenario: Outputs are available after apply

- **WHEN** `terraform apply` completes successfully
- **THEN** `module.sqs_<name>.queue_url` and `module.sqs_<name>.queue_arn` are non-empty strings

### Requirement: check-booking-pending-state queue is provisioned

The root `terraform/main.tf` SHALL instantiate the `modules/sqs` module to create the `check-booking-pending-state` queue, and `terraform/outputs.tf` SHALL expose its URL and ARN.

#### Scenario: Queue exists after terraform apply

- **WHEN** `terraform apply` is run on the root module
- **THEN** an SQS queue named `check-booking-pending-state` exists in the target AWS account and region
