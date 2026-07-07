# apigw-vpc-link Specification

## Purpose
Define the VPC Link v2 infrastructure managed within the api-gateway Terraform module, providing private network connectivity between API Gateway HTTP API and the internal NLB.

## Requirements

### Requirement: VPC Link v2 is created within the api-gateway module
The system SHALL create an `aws_apigatewayv2_vpc_link` resource inside `terraform/modules/api-gateway/`, serving as the private network bridge between API Gateway HTTP API and the internal NLB.

#### Scenario: VPC Link v2 resource exists after apply
- **WHEN** Terraform applies the api-gateway module
- **THEN** `aws_apigatewayv2_vpc_link` resource is created with status `AVAILABLE`

#### Scenario: VPC Link targets private subnets
- **WHEN** VPC Link v2 is configured
- **THEN** `subnet_ids` references `var.private_subnet_ids` (same subnets as the NLB)

### Requirement: api-gateway module receives VPC Link SG ID as input
The system SHALL receive the VPC Link Security Group ID via input variable `apigw_vpc_link_sg_id`, created and owned by the networking module, and attach it to the VPC Link.

#### Scenario: VPC Link SG is attached to the VPC Link
- **WHEN** VPC Link v2 is created
- **THEN** `security_group_ids` includes the value of `var.apigw_vpc_link_sg_id`

#### Scenario: api-gateway module does not create the VPC Link SG
- **WHEN** api-gateway module Terraform files are inspected
- **THEN** no `aws_security_group` resource exists in the module

### Requirement: VPC Link ID is exposed as module output
The system SHALL expose the VPC Link v2 ID as output `vpc_link_id` from the api-gateway module so it can be referenced in integration configuration.

#### Scenario: VPC Link ID output is available
- **WHEN** Terraform apply completes
- **THEN** `module.api_gateway.vpc_link_id` contains the VPC Link identifier

### Requirement: VPC Link has descriptive tags
The system SHALL tag the VPC Link v2 resource with common tags and a Name tag following `${var.name_prefix}-vpc-link` convention.

#### Scenario: VPC Link has Name tag
- **WHEN** VPC Link v2 is created
- **THEN** tags include `Name = "${var.name_prefix}-vpc-link"`

#### Scenario: VPC Link merges common tags
- **WHEN** VPC Link v2 is created
- **THEN** tags include all entries from `var.common_tags`
