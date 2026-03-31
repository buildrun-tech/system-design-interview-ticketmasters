## 1. Terraform image input contract

- [ ] 1.1 Add a root Terraform input for the workflow-selected ECS image tag or reference, with environment defaults preserved when no per-run override is supplied
- [ ] 1.2 Wire the selected image input through `terraform/main.tf` into the ECS module and render the task definition image from that value
- [ ] 1.3 Confirm the resulting Terraform plan changes only when the selected image input or other task definition inputs change

## 2. Workflow sequencing and handoff

- [ ] 2.1 Update `.github/workflows/deploy.yml` so the `dev` infrastructure stage waits for build, test, Docker build, and ECR push to finish successfully
- [ ] 2.2 Update `.github/workflows/deploy.yml` so the `prod` infrastructure stage waits for image promotion and passes the promoted image tag into Terraform
- [ ] 2.3 Align workflow outputs, environment variables, and ECR validation so Terraform always receives the image selected by the current pipeline run

## 3. Terraform-managed rollout verification

- [ ] 3.1 Replace the current deploy placeholder behavior with a post-apply verification/reporting stage that treats Terraform apply as the ECS deployment step
- [ ] 3.2 Verify the post-apply checks fail clearly when ECS does not adopt the expected task definition after Terraform apply
- [ ] 3.3 Validate the final `dev` and `prod` flows match the intended order and do not introduce a second imperative ECS deploy step
