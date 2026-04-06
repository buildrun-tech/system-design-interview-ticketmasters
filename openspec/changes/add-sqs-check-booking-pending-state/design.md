## Context

The Terraform infrastructure manages networking, RDS, and ECS via separate modules. There is no SQS module yet. The application's ECS task role already has full SQS permissions, but the queue itself is never created by Terraform — it would need to be created manually or by application code at runtime, which is inconsistent with the IaC approach.

## Goals / Non-Goals

**Goals:**
- Create a reusable `modules/sqs` Terraform module that provisions a standard SQS queue
- Provision the `check-booking-pending-state` queue using this module

**Non-Goals:**
- FIFO queues (not needed for this use case)
- Lambda event source mapping or consumer configuration
- Changes to IAM (ECS task role already covers SQS)

## Decisions

### Single-module approach for all SQS queues

**Decision:** One generic `modules/sqs` module, instantiated once per queue in `main.tf`.

**Rationale:** Follows the existing pattern (one `modules/ecs`, one `modules/rds`). Avoids duplicating queue resource definitions. New queues only require a new `module` block in `main.tf`.

**Alternatives considered:** Hardcoding the queue directly in `main.tf` without a module — rejected because it doesn't scale when more queues are added.

### Standard queue (not FIFO)

**Decision:** Use a standard SQS queue.

**Rationale:** Booking state checks are idempotent; at-least-once delivery with best-effort ordering is sufficient. FIFO queues have a lower throughput ceiling and are not needed here.

### Dead-letter queue (DLQ) included in module

**Decision:** The module creates a companion DLQ and configures a redrive policy by default.

**Rationale:** Prevents message loss from poison-pill messages without any extra effort from the caller. The DLQ is always created alongside the main queue.

## Risks / Trade-offs

- [Module is opinionated on DLQ] → Caller can disable via `max_receive_count = 0` variable if unwanted, but the simplest path always includes a DLQ.
- [Queue name is hardcoded at call-site] → Consistent with how `cluster_name` and `service_name` are passed to other modules; environment-specific naming is the caller's responsibility.

## Migration Plan

1. Add `terraform/modules/sqs/` with `main.tf`, `variables.tf`, `outputs.tf`
2. Add `module "sqs_check_booking_pending_state"` block to `terraform/main.tf`
3. Expose queue URL and ARN in `terraform/outputs.tf`
4. Run `terraform plan` to validate, then `terraform apply`
5. Rollback: `terraform destroy -target module.sqs_check_booking_pending_state` removes the queue and DLQ
