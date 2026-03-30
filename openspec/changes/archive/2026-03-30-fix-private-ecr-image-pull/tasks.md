## 1. Terraform image configuration

- [ ] 1.1 Add ECS module inputs for the image tag or equivalent image reference override
- [ ] 1.2 Update the ECS task definition to stop hardcoding `:latest` and use the configured image input
- [ ] 1.3 Wire root Terraform variables and environment defaults so dev and prod resolve to the published ECR tags

## 2. Deployment workflow alignment

- [ ] 2.1 Update `.github/workflows/deploy.yml` so the deployment step uses the same image tag contract as the image publishing step
- [ ] 2.2 Add an ECR image existence check for the configured repository and tag before ECS rollout begins

## 3. Validation

- [ ] 3.1 Review Terraform plans or rendered task definition output to confirm ECS references the expected environment-specific tag
- [ ] 3.2 Validate the deployment path fails early when the requested tag is missing and proceeds when the tag exists
- [ ] 3.3 Document the resolved image-tag behavior for operators troubleshooting ECS pull failures
