## Context

The ECS service runs on Fargate with `assign_public_ip = false`. The subnets attached to the service use a route table with `0.0.0.0/0` pointing to an Internet Gateway, so the subnets themselves are effectively public, but that does not give outbound internet access to task ENIs that only have private IP addresses. During task startup, ECS resolves `QUARKUS_DATASOURCE_PASSWORD` from AWS Secrets Manager through the task execution role, and the observed failure is a startup-time timeout to Secrets Manager rather than an IAM denial.

Today the Terraform layout separates security groups in `terraform/modules/networking` from ECS runtime resources in `terraform/modules/ecs`. The fix should preserve that separation while making the secret-fetch path explicit and testable.

## Goals / Non-Goals

**Goals:**
- Restore ECS task startup by allowing tasks to reach public AWS service endpoints
- Enable `assign_public_ip = true` so tasks can use the existing Internet Gateway route
- Preserve restricted inbound access through the ECS security group while allowing outbound connectivity
- Keep the change small and compatible with the current subnet and route-table layout

**Non-Goals:**
- Rework the broader VPC routing model or introduce a NAT gateway migration
- Change the database secret shape, naming, or IAM ownership model
- Add interface VPC endpoints for AWS services in this change
- Change application-level datasource configuration beyond unblocking secret retrieval

## Decisions

### Decision 1: Assign public IPs to ECS tasks

Change the ECS service network configuration to `assign_public_ip = true`. Because the attached subnets already route `0.0.0.0/0` to an Internet Gateway, this gives each task ENI outbound internet access and directly addresses the timeout seen during task initialization.

**Alternatives considered:**
- **Use an interface VPC endpoint for Secrets Manager:** rejected for this change because you explicitly want internet access from the task through a public IP rather than additional private endpoint infrastructure.
- **Rely on NAT/internet egress without public IPs:** rejected because the current deployment does not use NAT and the immediate blocker is missing public-IP assignment.
- **Inject the DB password as plain Terraform state or env var:** rejected because it weakens secret handling.

### Decision 2: Keep the ECS security group inbound rules unchanged

Public IP assignment is only for outbound connectivity. The ECS security group should continue allowing inbound application traffic only from the NLB security group, so the service does not become directly reachable from the internet.

**Alternatives considered:**
- **Broaden ECS inbound rules for direct public access:** rejected because public-IP egress does not require public inbound exposure.

### Decision 3: Keep the change focused on ECS service networking

The failure is caused by task ENIs lacking public internet reachability. Updating the ECS service network configuration is enough to address the problem, so this change should avoid adding endpoint resources, extra security groups, or new module outputs.

**Alternatives considered:**
- **Add configurable support for both endpoint and public-IP modes:** rejected to keep this change small and avoid unnecessary branching.

### Decision 4: Keep using the default public AWS service DNS names

Once tasks have public IPs, ECS can continue using the standard Secrets Manager endpoint without container configuration changes or endpoint-specific DNS wiring.

**Alternatives considered:**
- **Add custom endpoint configuration in the app or task definition:** rejected because it is unnecessary for standard AWS public endpoint access.

## Risks / Trade-offs

- **Tasks now have public IP addresses** -> Acceptable because outbound internet access is the desired operating model for this deployment
- **Public-IP consumption increases** -> Monitor subnet IP capacity if desired counts grow
- **Internet egress depends on the IGW path remaining present** -> Verify the subnet route table stays associated and unchanged
- **Other bootstrap failures may still exist** -> Validate task startup logs after rollout and treat any follow-on IAM or application issues separately

## Migration Plan

1. Update the ECS service network configuration in `terraform/modules/ecs/main.tf` to set `assign_public_ip = true`.
2. If needed, document or parameterize the public-IP behavior in `terraform/modules/ecs/variables.tf`.
3. Run `terraform plan` and verify the change is limited to ECS service networking.
4. Apply in the target environment and force a new ECS deployment so fresh tasks receive public IPs.
5. Validate that ECS tasks reach `RUNNING` and that the prior `ResourceInitializationError` no longer appears.

**Rollback:** revert `assign_public_ip` to `false` and redeploy the ECS service.

## Open Questions

- Should public IP assignment be always on for this service, or should it become an explicit environment-level variable?
- Should the ECS service eventually move to true private subnets with NAT or VPC endpoints once the immediate startup issue is resolved?
