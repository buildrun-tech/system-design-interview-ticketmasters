## MODIFIED Requirements

### Requirement: RDS security group accepts TCP 5432 from ECS security group
The system SHALL configure the RDS security group to accept inbound TCP traffic on port 5432 from the ECS tasks security group, the VPC CIDR, and the VPC default security group.

#### Scenario: RDS SG allows ECS SG on port 5432
- **WHEN** RDS security group is created
- **THEN** ingress rule allows TCP port 5432 from ECS security group ID (source_security_group_id)

#### Scenario: RDS SG ingress description references ECS
- **WHEN** RDS security group ingress rule is created
- **THEN** ingress rule description is "PostgreSQL from ECS" or similar

#### Scenario: RDS SG allows VPC CIDR on port 5432
- **WHEN** RDS security group is created
- **THEN** ingress rule allows TCP port 5432 from `data.aws_vpc.existing.cidr_block`

#### Scenario: RDS SG allows default SG on port 5432
- **WHEN** RDS security group is created
- **THEN** ingress rule allows TCP port 5432 from `data.aws_security_group.default.id`

#### Scenario: RDS SG has exactly three ingress rules
- **WHEN** RDS security group is created
- **THEN** RDS SG has three ingress rules: one from ECS SG, one from VPC CIDR, one from default SG
