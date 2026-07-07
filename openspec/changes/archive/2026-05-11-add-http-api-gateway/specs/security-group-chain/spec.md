## MODIFIED Requirements

### Requirement: NLB security group accepts TCP 8080 from VPC Link Security Group only
The system SHALL configure the NLB security group ingress rule to accept inbound TCP traffic on port 8080 exclusively from the API Gateway VPC Link Security Group ID, replacing the previous VPC CIDR-based rule.

#### Scenario: NLB SG allows VPC Link SG on port 8080
- **WHEN** NLB security group is created
- **THEN** ingress rule allows TCP port 8080 with `security_groups = [var.apigw_vpc_link_sg_id]`

#### Scenario: NLB SG does not allow VPC CIDR on port 8080
- **WHEN** NLB security group ingress rules are evaluated
- **THEN** there is no ingress rule using `cidr_blocks` for port 8080

#### Scenario: NLB SG ingress description references VPC Link
- **WHEN** NLB security group ingress rule is created
- **THEN** ingress rule description is "Traffic from API Gateway VPC Link" or similar

#### Scenario: NLB SG has exactly one ingress rule
- **WHEN** NLB security group ingress rules are evaluated
- **THEN** NLB SG has only one ingress rule (from VPC Link SG)

### Requirement: Networking module accepts VPC Link SG ID as input variable
The system SHALL add an input variable `apigw_vpc_link_sg_id` to the networking module to receive the VPC Link Security Group ID from the root module, enabling the NLB SG to reference it.

#### Scenario: Networking module has apigw_vpc_link_sg_id variable
- **WHEN** networking module variables are defined
- **THEN** `var.apigw_vpc_link_sg_id` exists with type `string`

#### Scenario: Root module passes VPC Link SG ID to networking module
- **WHEN** root main.tf instantiates the networking module
- **THEN** `apigw_vpc_link_sg_id = module.api_gateway.vpc_link_security_group_id` is passed as argument
