## 1. Create SQS Terraform Module

- [x] 1.1 Create `terraform/modules/sqs/variables.tf` with inputs: `queue_name`, `name_prefix`, `common_tags`, `visibility_timeout_seconds`, `message_retention_seconds`, `max_receive_count`
- [x] 1.2 Create `terraform/modules/sqs/main.tf` with `aws_sqs_queue` for the main queue and `aws_sqs_queue` for the DLQ, plus redrive policy
- [x] 1.3 Create `terraform/modules/sqs/outputs.tf` exposing `queue_url`, `queue_arn`, `dlq_url`, `dlq_arn`

## 2. Provision check-booking-pending-state Queue

- [x] 2.1 Add `module "sqs_check_booking_pending_state"` block to `terraform/main.tf` using the new module with queue name `check-booking-pending-state`
- [x] 2.2 Add `check_booking_pending_state_queue_url` and `check_booking_pending_state_queue_arn` outputs to `terraform/outputs.tf`

## 3. Validate

- [x] 3.1 Run `terraform validate` to confirm no syntax errors
- [ ] 3.2 Run `terraform plan` and verify the plan shows 3 new resources: main queue, DLQ, and redrive policy
