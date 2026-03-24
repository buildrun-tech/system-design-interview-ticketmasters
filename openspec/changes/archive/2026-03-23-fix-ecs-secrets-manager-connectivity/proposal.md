## Why

ECS tasks resolve the database password from AWS Secrets Manager during task initialization, but the current service configuration launches them with `assign_public_ip = false`. Even though the attached subnet route table is public and points `0.0.0.0/0` to an Internet Gateway, the task ENIs still cannot use that route without public IP addresses, which causes the observed `ResourceInitializationError` before the container can boot.

## What Changes

- Update the ECS service so tasks receive public IPs and can use the existing Internet Gateway route for outbound internet access
- Preserve the current secret reference flow so task initialization can fetch `QUARKUS_DATASOURCE_PASSWORD` from AWS Secrets Manager over the public AWS endpoint
- Clarify ECS networking requirements for workloads running in public subnets with public-IP egress
- Add validation guidance to confirm ECS task startup succeeds after the networking change

## Capabilities

### New Capabilities
- `ecs-public-egress`: Provide outbound internet access for ECS tasks by assigning public IPs in IGW-routed subnets

### Modified Capabilities
- `security-group-chain`: Extend internal security group requirements to cover ECS tasks that keep restricted inbound access while using public-IP outbound connectivity

## Impact

- `terraform/modules/ecs/main.tf`: update ECS service networking to assign public IPs to task ENIs
- `terraform/modules/ecs/variables.tf`: add or document any toggle controlling ECS public IP assignment
- Runtime impact: new ECS deployments can fetch the DB password secret by using public-IP outbound connectivity through the Internet Gateway
