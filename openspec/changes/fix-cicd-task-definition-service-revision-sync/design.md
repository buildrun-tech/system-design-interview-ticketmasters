## Context

The ECS module currently creates a new task definition revision when its rendered container definition changes, but the ECS service resource is configured with `ignore_changes = [task_definition]`. That suppresses the service update Terraform would normally perform, so CI/CD can finish with a newer task definition revision registered in ECS while the service keeps running the previous revision. The fix spans Terraform state behavior and deployment verification in GitHub Actions, so the rollout contract needs to be explicit before implementation.

## Goals / Non-Goals

**Goals:**
- Ensure Terraform-managed ECS services advance to the task definition revision created during the same deployment
- Make the deployment pipeline verify that the service revision matches the expected task definition after apply
- Preserve a safe rollback path by keeping ECS service updates declarative and observable

**Non-Goals:**
- Rework the broader CI/CD flow into a separate imperative ECS deployment process
- Change unrelated ECS networking, IAM, or image publishing behavior
- Introduce blue/green deployment orchestration or CodeDeploy-based rollouts

## Decisions

### Let Terraform manage the ECS service task definition reference
The ECS service will stop ignoring `task_definition` changes so Terraform can update the service whenever a new revision is registered. This keeps the source of truth in Terraform and directly addresses the current drift where the latest revision exists but is never adopted by the service.

Alternative considered: keeping `ignore_changes` and adding a separate AWS CLI `update-service` step in CI. That can force a rollout, but it splits ownership between Terraform and imperative deployment logic, increasing drift risk.

### Add post-apply revision verification in CI/CD
The pipeline will query ECS after Terraform apply and confirm the service points to the expected task definition ARN or revision. This makes the failure mode visible immediately instead of requiring operators to notice a stale service in the ECS console.

Alternative considered: relying on Terraform exit status alone. Terraform may still succeed at registering a new task definition, so success alone is not enough to prove the service rolled forward.

### Keep rollout behavior tied to task definition changes rather than forcing every deployment
The design should update the ECS service when the task definition changes and avoid unconditional restarts on no-op deploys. This preserves predictable Terraform plans and prevents unnecessary service churn.

Alternative considered: enabling forced redeployments on every pipeline run. That may mask the original synchronization bug and causes avoidable restarts even when there is no new revision to deploy.

## Risks / Trade-offs

- Service updates may now occur more often when task definition inputs change -> Mitigation: rely on Terraform plans and ECS rolling update settings to review and control impact
- CI verification can fail due to eventual consistency shortly after apply -> Mitigation: add bounded retry logic before declaring the service out of sync
- Existing operators may be used to inactive extra revisions accumulating without service rollout -> Mitigation: document the new expectation that the active service revision should match the latest Terraform-managed revision after deploy

## Migration Plan

1. Remove or narrow the ECS service lifecycle rule that ignores `task_definition` changes.
2. Apply Terraform in dev and confirm the ECS service updates to the new task definition revision.
3. Add post-apply workflow checks that compare the expected revision to the service's active revision.
4. Roll back by restoring the prior lifecycle behavior only if service updates prove unstable, while noting that this reintroduces stale-revision drift.

## Open Questions

- Should the workflow verify only the service's configured task definition ARN, or also wait for the deployment to stabilize on the new revision before reporting success?
