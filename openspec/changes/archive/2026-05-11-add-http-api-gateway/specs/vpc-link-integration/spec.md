## MODIFIED Requirements

### Requirement: VPC Link connects API Gateway HTTP API v2 to NLB
The system SHALL create an AWS API Gateway VPC Link using `aws_apigatewayv2_vpc_link` (HTTP API v2 type) that enables private communication from API Gateway to the internal Network Load Balancer within the VPC.

#### Scenario: VPC Link v2 resource is created successfully
- **WHEN** Terraform applies the api-gateway module
- **THEN** `aws_apigatewayv2_vpc_link` resource is created with status `AVAILABLE`

#### Scenario: VPC Link v2 is created in the api-gateway module
- **WHEN** infrastructure is applied
- **THEN** the VPC Link resource lives in `terraform/modules/api-gateway/`, not in `terraform/modules/ecs/`

### Requirement: Legacy REST API v1 VPC Link is removed from ECS module
The system SHALL remove the `aws_api_gateway_vpc_link` resource from `terraform/modules/ecs/main.tf`, as it is superseded by the VPC Link v2 in the api-gateway module.

#### Scenario: ECS module has no aws_api_gateway_vpc_link resource
- **WHEN** ECS module Terraform files are inspected
- **THEN** no `aws_api_gateway_vpc_link` resource exists in the ECS module

#### Scenario: Legacy VPC Link is removed from Terraform state before apply
- **WHEN** migration plan is executed
- **THEN** `terraform state rm module.ecs.aws_api_gateway_vpc_link.main` is run prior to applying the ECS module changes

### Requirement: VPC Link v2 has a Security Group
The system SHALL configure the `aws_apigatewayv2_vpc_link` with `security_group_ids` referencing the dedicated VPC Link SG, replacing the previous model where the VPC Link had no security group.

#### Scenario: VPC Link v2 security_group_ids is set
- **WHEN** VPC Link v2 is created
- **THEN** `security_group_ids` contains exactly one SG ID: the VPC Link SG

### Requirement: VPC Link v2 deploys ENIs in private subnets
The system SHALL configure VPC Link v2 to create Elastic Network Interfaces (ENIs) in the private subnets where the NLB is deployed.

#### Scenario: ENIs are created in correct subnets
- **WHEN** VPC Link v2 is created
- **THEN** ENIs are deployed in the same private subnets as the NLB (`var.private_subnet_ids`)

### Requirement: VPC Link ID is exposed as output
The system SHALL expose the VPC Link v2 ID as a Terraform output `vpc_link_id` from the api-gateway module to enable API Gateway integration configuration.

#### Scenario: VPC Link ID is available as module output
- **WHEN** Terraform apply completes successfully
- **THEN** `module.api_gateway.vpc_link_id` contains the VPC Link v2 identifier

## REMOVED Requirements

### Requirement: VPC Link supports REST API integration
**Reason**: The system has migrated from REST API v1 to HTTP API v2. The `aws_api_gateway_vpc_link` resource (REST API type) is replaced by `aws_apigatewayv2_vpc_link` (HTTP API type), which has different capabilities including support for Security Groups.
**Migration**: No action required for consumers — the VPC Link is an internal infrastructure detail. The API Gateway module now manages its own VPC Link v2.

### Requirement: VPC Link ID is exposed as output (from ECS module)
**Reason**: The VPC Link has moved from the ECS module to the api-gateway module. The output `vpc_link_id` is now available from `module.api_gateway.vpc_link_id`, not from the ECS module.
**Migration**: Any reference to `module.ecs.vpc_link_id` should be updated to `module.api_gateway.vpc_link_id`.
