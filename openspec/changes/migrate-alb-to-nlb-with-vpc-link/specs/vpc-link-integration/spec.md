## ADDED Requirements

### Requirement: VPC Link connects API Gateway to NLB
The system SHALL create an AWS API Gateway VPC Link that enables private communication from API Gateway to the internal Network Load Balancer within the VPC.

#### Scenario: VPC Link is created successfully
- **WHEN** Terraform applies the infrastructure
- **THEN** VPC Link resource is created with status "AVAILABLE"

#### Scenario: VPC Link targets the NLB
- **WHEN** VPC Link is configured
- **THEN** VPC Link target ARN points to the NLB ARN

### Requirement: VPC Link deploys ENIs in private subnets
The system SHALL configure VPC Link to create Elastic Network Interfaces (ENIs) in the private subnets where the NLB is deployed.

#### Scenario: ENIs are created in correct subnets
- **WHEN** VPC Link is created
- **THEN** ENIs are deployed in the same private subnets as the NLB

### Requirement: VPC Link has identifiable name
The system SHALL name the VPC Link using the naming convention `${var.name_prefix}-vpc-link` for easy identification in AWS console and API Gateway configuration.

#### Scenario: VPC Link name follows convention
- **WHEN** VPC Link is created
- **THEN** VPC Link name is `${var.name_prefix}-vpc-link`

### Requirement: VPC Link ID is exposed as output
The system SHALL expose the VPC Link ID as a Terraform output to enable API Gateway team to configure the integration.

#### Scenario: VPC Link ID is available
- **WHEN** Terraform apply completes successfully
- **THEN** output `vpc_link_id` contains the VPC Link identifier

#### Scenario: VPC Link ARN is available
- **WHEN** Terraform apply completes successfully
- **THEN** output `vpc_link_arn` contains the VPC Link ARN

### Requirement: VPC Link supports REST API integration
The system SHALL create VPC Link compatible with API Gateway REST API integration (not HTTP API).

#### Scenario: VPC Link type is correct
- **WHEN** VPC Link is created
- **THEN** VPC Link supports API Gateway REST API (not HTTP API v2)

### Requirement: VPC Link has descriptive metadata
The system SHALL tag the VPC Link with common tags and descriptive information for resource management and cost tracking.

#### Scenario: VPC Link has tags
- **WHEN** VPC Link is created
- **THEN** VPC Link includes `var.common_tags` and Name tag
