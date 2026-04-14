## ADDED Requirements

### Requirement: VPC Link v2 is created within the api-gateway module
The system SHALL create an `aws_apigatewayv2_vpc_link` resource inside `terraform/modules/api-gateway/`, serving as the private network bridge between API Gateway HTTP API and the internal NLB.

#### Scenario: VPC Link v2 resource exists after apply
- **WHEN** Terraform applies the api-gateway module
- **THEN** `aws_apigatewayv2_vpc_link` resource is created with status `AVAILABLE`

#### Scenario: VPC Link targets private subnets
- **WHEN** VPC Link v2 is configured
- **THEN** `subnet_ids` references `var.private_subnet_ids` (same subnets as the NLB)

### Requirement: VPC Link v2 has a dedicated Security Group
The system SHALL create an `aws_security_group` (`apigw-vpc-link-sg`) within the api-gateway module, attached to the VPC Link v2, to control egress to the NLB.

#### Scenario: VPC Link SG is created
- **WHEN** api-gateway module is applied
- **THEN** a security group named `${var.name_prefix}-apigw-vpc-link-sg` exists in the VPC

#### Scenario: VPC Link SG is attached to the VPC Link
- **WHEN** VPC Link v2 is created
- **THEN** `security_group_ids` includes the VPC Link SG ID

### Requirement: VPC Link Security Group egress is restricted to NLB Security Group
The system SHALL configure the VPC Link SG egress to allow TCP traffic on port 8080 only to the NLB Security Group, preventing the VPC Link from initiating connections to other targets.

#### Scenario: VPC Link SG egress allows port 8080 to NLB SG
- **WHEN** VPC Link SG is created
- **THEN** egress rule allows TCP port 8080 with `security_groups = [var.nlb_security_group_id]`

#### Scenario: VPC Link SG has no unrestricted egress
- **WHEN** VPC Link SG egress rules are evaluated
- **THEN** there is no egress rule allowing `0.0.0.0/0`

### Requirement: VPC Link Security Group has no inbound rules
The system SHALL configure the VPC Link SG with no ingress rules, as API Gateway manages its own injection of traffic into the ENIs — no external source needs to reach the VPC Link SG.

#### Scenario: VPC Link SG has no ingress rules
- **WHEN** VPC Link SG ingress rules are evaluated
- **THEN** there are no ingress rules defined

### Requirement: VPC Link ID is exposed as module output
The system SHALL expose the VPC Link v2 ID as output `vpc_link_id` from the api-gateway module so it can be referenced in integration configuration.

#### Scenario: VPC Link ID output is available
- **WHEN** Terraform apply completes
- **THEN** `module.api_gateway.vpc_link_id` contains the VPC Link identifier

### Requirement: VPC Link SG ID is exposed as module output
The system SHALL expose the VPC Link SG ID as output `vpc_link_security_group_id` so the networking module can reference it when configuring NLB SG ingress rules.

#### Scenario: VPC Link SG ID output is available
- **WHEN** Terraform apply completes
- **THEN** `module.api_gateway.vpc_link_security_group_id` contains the SG identifier

### Requirement: VPC Link has descriptive tags
The system SHALL tag the VPC Link v2 resource with common tags and a Name tag following `${var.name_prefix}-vpc-link` convention.

#### Scenario: VPC Link has Name tag
- **WHEN** VPC Link v2 is created
- **THEN** tags include `Name = "${var.name_prefix}-vpc-link"`

#### Scenario: VPC Link merges common tags
- **WHEN** VPC Link v2 is created
- **THEN** tags include all entries from `var.common_tags`

### Requirement: VPC Link SG uses lifecycle create_before_destroy
The system SHALL configure the VPC Link SG with `lifecycle { create_before_destroy = true }` to prevent disruption during updates.

#### Scenario: VPC Link SG has lifecycle policy
- **WHEN** VPC Link SG resource is defined
- **THEN** resource includes `lifecycle { create_before_destroy = true }`
