## 1. Update ECS networking for public internet egress

- [x] 1.1 Update the ECS service network configuration in `terraform/modules/ecs/main.tf` to set `assign_public_ip = true`
- [x] 1.2 If needed, add or document an ECS module input in `terraform/modules/ecs/variables.tf` for public IP assignment behavior
- [x] 1.3 Keep ECS task ingress restricted to the NLB security group while preserving outbound access

## 2. Validate ECS secret bootstrap over public endpoints

- [x] 2.1 Confirm the task definition still sources `QUARKUS_DATASOURCE_PASSWORD` from `var.db_password_secret_arn`
- [ ] 2.2 Run `terraform fmt -recursive` and `terraform validate`
- [ ] 2.3 Review `terraform plan` to verify the change is limited to ECS service networking

## 3. Deploy and verify task startup

- [ ] 3.1 Apply the change in the target environment and force a new ECS deployment
- [ ] 3.2 Verify new ECS tasks receive public IP addresses
- [ ] 3.3 Verify ECS tasks reach `RUNNING` without the previous `ResourceInitializationError` from Secrets Manager
