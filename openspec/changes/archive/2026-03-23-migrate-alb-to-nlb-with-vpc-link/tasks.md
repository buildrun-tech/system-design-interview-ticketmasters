## 1. Update Networking Module Security Groups

- [x] 1.1 Rename `aws_security_group.alb` resource to `aws_security_group.nlb` in `terraform/modules/networking/main.tf`
- [x] 1.2 Update NLB security group name_prefix from `"${var.name_prefix}-alb-"` to `"${var.name_prefix}-nlb-"`
- [x] 1.3 Remove HTTP (port 80) and HTTPS (port 443) ingress rules from NLB security group
- [x] 1.4 Add TCP port 8080 ingress rule to NLB security group with source `cidr_blocks = [data.aws_vpc.existing.cidr_block]`
- [x] 1.5 Update NLB security group ingress description to "Traffic from VPC"
- [x] 1.6 Ensure NLB security group has explicit egress rule allowing all traffic (0.0.0.0/0)
- [x] 1.7 Update NLB security group Name tag from `"${var.name_prefix}-alb-sg"` to `"${var.name_prefix}-nlb-sg"`
- [x] 1.8 Update ECS security group ingress rule to reference `aws_security_group.nlb.id` instead of `aws_security_group.alb.id`
- [x] 1.9 Update ECS security group ingress description from "HTTP from ALB" to "HTTP from NLB"
- [x] 1.10 Verify RDS security group remains unchanged (still references ECS security group)

## 2. Update Networking Module Outputs

- [x] 2.1 Rename output `alb_security_group_id` to `nlb_security_group_id` in `terraform/modules/networking/outputs.tf`
- [x] 2.2 Update output value from `aws_security_group.alb.id` to `aws_security_group.nlb.id`
- [x] 2.3 Update output description from "Security group ID for ALB" to "Security group ID for NLB"
- [x] 2.4 Verify `ecs_security_group_id` and `rds_security_group_id` outputs remain unchanged

## 3. Update Networking Module Variables

- [x] 3.1 Remove or update `allowed_cidr_blocks` variable description in `terraform/modules/networking/variables.tf` (no longer used for NLB ingress)
- [x] 3.2 Verify `vpc_id`, `container_port`, and `db_port` variables exist and are correct

## 4. Remove ALB from ECS Module

- [x] 4.1 Remove `aws_lb.main` resource (ALB) from `terraform/modules/ecs/main.tf`
- [x] 4.2 Remove `aws_lb_listener.http` resource if exists
- [x] 4.3 Remove `aws_lb_listener.https` resource if exists
- [x] 4.4 Remove ALB-specific listener rules and configurations

## 5. Add NLB to ECS Module

- [x] 5.1 Create new `aws_lb.main` resource with `load_balancer_type = "network"` in `terraform/modules/ecs/main.tf`
- [x] 5.2 Configure NLB with `internal = true` to make it private
- [x] 5.3 Set NLB subnets to `var.private_subnet_ids` (change from public_subnet_ids)
- [x] 5.4 Set NLB security_groups to `[var.nlb_security_group_id]`
- [x] 5.5 Enable cross-zone load balancing: `enable_cross_zone_load_balancing = true`
- [x] 5.6 Add NLB name: `name = "${var.name_prefix}-nlb"` or similar
- [x] 5.7 Add NLB tags: `merge(var.common_tags, { Name = "${var.name_prefix}-nlb" })`
- [x] 5.8 Configure `enable_deletion_protection` based on environment (optional variable)

## 6. Update Target Group Configuration

- [x] 6.1 Update `aws_lb_target_group.main` protocol from "HTTP" to "TCP"
- [x] 6.2 Set `target_type = "ip"` in target group
- [x] 6.3 Keep target group port as `var.container_port` (8080)
- [x] 6.4 Update health check protocol to "TCP"
- [x] 6.5 Set health check port to "traffic-port"
- [x] 6.6 Remove HTTP-specific health check parameters (path, matcher)
- [x] 6.7 Set health check `healthy_threshold = 3`
- [x] 6.8 Set health check `unhealthy_threshold = 3`
- [x] 6.9 Set health check `interval = 30`
- [x] 6.10 Set `deregistration_delay = 30` for graceful shutdown
- [x] 6.11 Update target group name to reflect NLB usage

## 7. Update NLB Listener

- [x] 7.1 Create new `aws_lb_listener.main` for NLB on port 8080
- [x] 7.2 Set listener protocol to "TCP"
- [x] 7.3 Set listener port to 8080
- [x] 7.4 Configure default action to forward to target group ARN
- [x] 7.5 Remove any HTTP/HTTPS listener-specific configurations

## 8. Add VPC Link Resource

- [x] 8.1 Create `aws_api_gateway_vpc_link.main` resource in `terraform/modules/ecs/main.tf`
- [x] 8.2 Set VPC Link name: `name = "${var.name_prefix}-vpc-link"`
- [x] 8.3 Set VPC Link description: `description = "VPC Link for API Gateway to access internal NLB"`
- [x] 8.4 Set VPC Link target_arns to `[aws_lb.main.arn]` (NLB ARN)
- [x] 8.5 Add VPC Link tags: `merge(var.common_tags, { Name = "${var.name_prefix}-vpc-link" })`

## 9. Update ECS Module Variables

- [x] 9.1 Rename variable `alb_security_group_id` to `nlb_security_group_id` in `terraform/modules/ecs/variables.tf`
- [x] 9.2 Update variable description from "Security group ID for ALB" to "Security group ID for NLB"
- [x] 9.3 Remove or rename `public_subnet_ids` variable (NLB uses private subnets)
- [x] 9.4 Remove `alb_name` variable (no longer needed)
- [x] 9.5 Remove HTTP/HTTPS health check variables if no longer applicable
- [x] 9.6 Keep `private_subnet_ids`, `container_port`, and other ECS-related variables

## 10. Update ECS Module Outputs

- [x] 10.1 Remove output `alb_dns_name` from `terraform/modules/ecs/outputs.tf`
- [x] 10.2 Remove output `alb_arn` if exists
- [x] 10.3 Remove output `alb_zone_id` if exists
- [x] 10.4 Add output `nlb_dns_name` with value `aws_lb.main.dns_name`
- [x] 10.5 Add output `nlb_arn` with value `aws_lb.main.arn`
- [x] 10.6 Add output `nlb_zone_id` with value `aws_lb.main.zone_id`
- [x] 10.7 Add output `vpc_link_id` with value `aws_api_gateway_vpc_link.main.id`
- [x] 10.8 Add output `vpc_link_arn` with value `aws_api_gateway_vpc_link.main.arn` (optional)

## 11. Update Root Module Configuration

- [x] 11.1 Update `terraform/main.tf` to pass `nlb_security_group_id` from networking module to ecs module
- [x] 11.2 Change reference from `module.networking.alb_security_group_id` to `module.networking.nlb_security_group_id`
- [x] 11.3 Update subnet parameter for ecs module to use private subnets (not public) for NLB
- [x] 11.4 Remove any ALB-specific variable passing
- [x] 11.5 Verify all module inputs are correctly wired

## 12. Update Root Module Outputs

- [x] 12.1 Remove `alb_dns_name` output from root `terraform/outputs.tf` if exists
- [x] 12.2 Add `nlb_dns_name` output with value `module.ecs.nlb_dns_name`
- [x] 12.3 Add `nlb_arn` output with value `module.ecs.nlb_arn`
- [x] 12.4 Add `vpc_link_id` output with value `module.ecs.vpc_link_id`
- [x] 12.5 Add `vpc_link_arn` output if needed for API Gateway configuration

## 13. Validate Terraform Configuration

- [x] 13.1 Run `terraform fmt -recursive` to format all Terraform files
- [x] 13.2 Run `terraform validate` to check for syntax errors
- [x] 13.3 Run `terraform plan` to preview infrastructure changes
- [x] 13.4 Review plan output for expected destroy (ALB) and create (NLB, VPC Link) operations
- [x] 13.5 Verify security group changes show correct resource replacement
- [x] 13.6 Check for any unexpected resource deletions or modifications

## 14. Deploy and Test

- [ ] 14.1 Deploy to staging/dev environment: `terraform apply`
- [ ] 14.2 Verify NLB is created with status "active"
- [ ] 14.3 Verify VPC Link is created with status "available"
- [ ] 14.4 Verify NLB target group shows healthy targets (ECS tasks registered)
- [ ] 14.5 Check NLB security group rules in AWS console
- [ ] 14.6 Check ECS security group ingress rule references NLB SG
- [ ] 14.7 Check RDS security group ingress rule references ECS SG (unchanged)
- [ ] 14.8 Test connectivity: API Gateway → VPC Link → NLB → ECS (requires API Gateway setup)
- [ ] 14.9 Verify database connectivity from ECS tasks to RDS
- [ ] 14.10 Monitor CloudWatch metrics for NLB (HealthyHostCount, UnHealthyHostCount)
- [ ] 14.11 Verify cross-zone load balancing is distributing traffic correctly

## 15. Documentation and Cleanup

- [ ] 15.1 Update README or architecture documentation with NLB + VPC Link architecture
- [ ] 15.2 Document VPC Link ID for API Gateway team
- [ ] 15.3 Update any runbooks or operational procedures referencing ALB
- [ ] 15.4 Update monitoring dashboards to use NLB metrics instead of ALB metrics
- [ ] 15.5 Update CloudWatch alarms to monitor NLB health checks
- [ ] 15.6 Remove any ALB-specific documentation or comments from code
- [ ] 15.7 Verify Terraform state is clean (no orphaned resources)
