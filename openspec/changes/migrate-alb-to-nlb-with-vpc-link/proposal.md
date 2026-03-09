## Why

The current architecture uses an Application Load Balancer (ALB) for public internet access, but we need to integrate with API Gateway using VPC Link for private API access. VPC Link requires a Network Load Balancer (NLB) to route traffic internally within the VPC. This migration enables secure, private API Gateway integration while maintaining proper security group isolation across all layers (NLB → ECS → RDS).

## What Changes

- **BREAKING**: Remove ALB infrastructure and replace with internal NLB
- **BREAKING**: Replace ALB security group with NLB security group in networking module
- Add VPC Link resource for API Gateway integration
- Add NLB with target group pointing to ECS service IP targets on port 8080
- Update security group chain: NLB SG → ECS SG → RDS SG (security group reference-based rules)
- Update ECS service to reference NLB target group instead of ALB target group
- Remove ALB-related outputs and replace with NLB outputs

## Capabilities

### New Capabilities
- `vpc-link-integration`: VPC Link configuration for API Gateway to privately access NLB within VPC
- `nlb-routing`: Network Load Balancer setup with target group routing to ECS service on port 8080
- `security-group-chain`: Three-tier security group isolation (NLB SG → ECS SG → RDS SG) with reference-based rules

### Modified Capabilities
<!-- No existing capabilities are being modified - this is new infrastructure -->

## Impact

**Infrastructure:**
- `terraform/modules/networking/main.tf`: Replace `aws_security_group.alb` with `aws_security_group.nlb`
- `terraform/modules/networking/outputs.tf`: Replace ALB SG outputs with NLB SG outputs
- `terraform/modules/ecs/main.tf`: Remove ALB resource, add NLB + target group + VPC Link
- `terraform/modules/ecs/variables.tf`: Update variables for NLB configuration
- `terraform/modules/ecs/outputs.tf`: Replace ALB outputs with NLB outputs

**Breaking Changes:**
- ALB will be destroyed and replaced with NLB (requires DNS/API Gateway reconfiguration)
- Output names change from `alb_*` to `nlb_*` (may affect dependent modules or scripts)
- Load balancer DNS endpoint changes (requires API Gateway VPC Link update)

**Runtime Impact:**
- Requires coordinated deployment: deploy infrastructure → configure API Gateway VPC Link → cutover traffic
- Brief downtime expected during ALB → NLB migration
