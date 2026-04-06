## Why

The application needs an SQS queue named `check-booking-pending-state` to support asynchronous booking state verification flows. Currently, there is no Terraform module for SQS, so queue provisioning is missing from infrastructure-as-code.

## What Changes

- New Terraform module `modules/sqs` for creating and configuring SQS standard queues
- Instantiation of the `check-booking-pending-state` queue using the new module in `terraform/main.tf`
- Output of the queue URL and ARN from the root module for use by other resources

## Capabilities

### New Capabilities

- `sqs-module`: Reusable Terraform module that provisions an SQS standard queue with configurable visibility timeout, message retention, and dead-letter queue support

### Modified Capabilities

<!-- none -->

## Impact

- New directory: `terraform/modules/sqs/`
- Modified file: `terraform/main.tf` (adds `module "sqs_check_booking_pending_state"` block)
- Modified file: `terraform/outputs.tf` (exposes queue URL and ARN)
- The ECS task role already has broad SQS permissions (`sqs:*` on `*`), so no IAM changes are required
