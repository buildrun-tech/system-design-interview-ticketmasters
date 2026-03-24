## 1. Terraform ECS service synchronization

- [ ] 1.1 Remove or narrow the ECS service lifecycle rule in `terraform/modules/ecs/main.tf` that ignores `task_definition` updates
- [ ] 1.2 Confirm the ECS service still references the Terraform-managed task definition ARN so a new revision updates the service during apply
- [ ] 1.3 Review the resulting Terraform plan behavior to verify no-op runs do not trigger unnecessary ECS redeployments

## 2. CI/CD deployment verification

- [ ] 2.1 Update `.github/workflows/deploy.yml` to capture the expected task definition revision after Terraform apply
- [ ] 2.2 Add a post-apply ECS check that compares the service's active task definition revision with the expected revision
- [ ] 2.3 Fail the workflow with a clear synchronization error when the ECS service remains on an older revision after deployment

## 3. Validation and operator guidance

- [ ] 3.1 Validate in dev that a task definition change produces both a new revision and an ECS service update to that revision
- [ ] 3.2 Validate that a no-change deployment does not force an unnecessary ECS service rollout
- [ ] 3.3 Document how to verify the active ECS service revision when troubleshooting future deployment issues
