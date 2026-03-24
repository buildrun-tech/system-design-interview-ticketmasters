## ADDED Requirements

### Requirement: NLB security group accepts TCP 8080 from VPC CIDR
The system SHALL create a security group for the NLB that accepts inbound TCP traffic on port 8080 from the VPC CIDR block.

#### Scenario: NLB SG allows VPC CIDR on port 8080
- **WHEN** NLB security group is created
- **THEN** ingress rule allows TCP port 8080 from VPC CIDR (data.aws_vpc.existing.cidr_block)

#### Scenario: NLB SG ingress description is clear
- **WHEN** NLB security group ingress rule is created
- **THEN** ingress rule description is "Traffic from VPC" or similar

### Requirement: NLB security group allows all outbound traffic
The system SHALL configure the NLB security group to allow all outbound traffic for forwarding requests to ECS targets.

#### Scenario: NLB SG egress is unrestricted
- **WHEN** NLB security group is created
- **THEN** egress rule allows all protocols (0.0.0.0/0)

### Requirement: ECS security group accepts TCP 8080 from NLB security group
The system SHALL configure the ECS tasks security group to accept inbound TCP traffic on port 8080 only from the NLB security group.

#### Scenario: ECS SG allows NLB SG on port 8080
- **WHEN** ECS security group is created
- **THEN** ingress rule allows TCP port 8080 from NLB security group ID (source_security_group_id)

#### Scenario: ECS SG ingress description references NLB
- **WHEN** ECS security group ingress rule is created
- **THEN** ingress rule description is "HTTP from NLB" or similar

#### Scenario: ECS SG does not accept traffic from other sources
- **WHEN** ECS security group is created
- **THEN** ECS SG has only one ingress rule (from NLB SG)

### Requirement: ECS security group allows all outbound traffic
The system SHALL configure the ECS tasks security group to allow all outbound traffic for database connections and external API calls.

#### Scenario: ECS SG egress is unrestricted
- **WHEN** ECS security group is created
- **THEN** egress rule allows all protocols (0.0.0.0/0)

#### Scenario: ECS SG permits outbound HTTPS to public AWS endpoints
- **WHEN** ECS tasks initiate HTTPS connections to public AWS service endpoints such as Secrets Manager
- **THEN** the ECS security group permits the outbound connection

### Requirement: ECS security group preserves restricted inbound access with public-IP egress
The system SHALL keep ECS task inbound access restricted to the NLB security group even when ECS tasks receive public IP addresses for outbound internet connectivity.

#### Scenario: ECS inbound rule still references the NLB security group
- **WHEN** the ECS security group ingress rules are rendered
- **THEN** inbound application traffic is allowed from `aws_security_group.nlb.id` on the container port

#### Scenario: ECS tasks are not opened directly to the internet
- **WHEN** the ECS security group ingress rules are evaluated
- **THEN** access is not granted from `0.0.0.0/0` or the full VPC CIDR block

### Requirement: RDS security group accepts TCP 5432 from ECS security group
The system SHALL configure the RDS security group to accept inbound TCP traffic on port 5432 only from the ECS tasks security group.

#### Scenario: RDS SG allows ECS SG on port 5432
- **WHEN** RDS security group is created
- **THEN** ingress rule allows TCP port 5432 from ECS security group ID (source_security_group_id)

#### Scenario: RDS SG ingress description references ECS
- **WHEN** RDS security group ingress rule is created
- **THEN** ingress rule description is "PostgreSQL from ECS" or similar

#### Scenario: RDS SG does not accept traffic from other sources
- **WHEN** RDS security group is created
- **THEN** RDS SG has only one ingress rule (from ECS SG)

### Requirement: Security groups use reference-based rules for internal traffic
The system SHALL use security group ID references (not CIDR blocks) for traffic between NLB, ECS, and RDS to maintain isolation as IP addresses change.

#### Scenario: ECS SG references NLB SG by ID
- **WHEN** ECS security group ingress rule is created
- **THEN** rule uses `security_groups = [aws_security_group.nlb.id]` not CIDR blocks

#### Scenario: RDS SG references ECS SG by ID
- **WHEN** RDS security group ingress rule is created
- **THEN** rule uses `security_groups = [aws_security_group.ecs_tasks.id]` not CIDR blocks

### Requirement: Security groups have lifecycle policy
The system SHALL configure security groups with `create_before_destroy = true` lifecycle policy to prevent disruption during updates.

#### Scenario: NLB SG has lifecycle policy
- **WHEN** NLB security group is created
- **THEN** resource includes `lifecycle { create_before_destroy = true }`

#### Scenario: ECS SG has lifecycle policy
- **WHEN** ECS security group is created
- **THEN** resource includes `lifecycle { create_before_destroy = true }`

#### Scenario: RDS SG has lifecycle policy
- **WHEN** RDS security group is created
- **THEN** resource includes `lifecycle { create_before_destroy = true }`

### Requirement: Security groups use name prefix for uniqueness
The system SHALL create security groups with `name_prefix` instead of `name` to allow multiple instances and avoid naming conflicts.

#### Scenario: NLB SG uses name prefix
- **WHEN** NLB security group is created
- **THEN** resource uses `name_prefix = "${var.name_prefix}-nlb-"`

#### Scenario: ECS SG uses name prefix
- **WHEN** ECS security group is created
- **THEN** resource uses `name_prefix = "${var.name_prefix}-ecs-tasks-"`

#### Scenario: RDS SG uses name prefix
- **WHEN** RDS security group is created
- **THEN** resource uses `name_prefix = "${var.name_prefix}-rds-"`

### Requirement: Security groups have descriptive tags
The system SHALL tag all security groups with common tags and descriptive names for resource identification.

#### Scenario: NLB SG has Name tag
- **WHEN** NLB security group is created
- **THEN** tags include Name = "${var.name_prefix}-nlb-sg"

#### Scenario: ECS SG has Name tag
- **WHEN** ECS security group is created
- **THEN** tags include Name = "${var.name_prefix}-ecs-tasks-sg"

#### Scenario: RDS SG has Name tag
- **WHEN** RDS security group is created
- **THEN** tags include Name = "${var.name_prefix}-rds-sg"

#### Scenario: Security groups merge common tags
- **WHEN** any security group is created
- **THEN** tags include `merge(var.common_tags, { Name = "..." })`

### Requirement: Security group outputs are exposed
The system SHALL expose all security group IDs as Terraform outputs for use by dependent modules.

#### Scenario: NLB SG ID is available
- **WHEN** networking module apply completes
- **THEN** output `nlb_security_group_id` contains the NLB SG ID

#### Scenario: ECS SG ID is available
- **WHEN** networking module apply completes
- **THEN** output `ecs_security_group_id` contains the ECS SG ID

#### Scenario: RDS SG ID is available
- **WHEN** networking module apply completes
- **THEN** output `rds_security_group_id` contains the RDS SG ID

### Requirement: No circular security group dependencies
The system SHALL configure security group references in one direction only to prevent Terraform dependency cycles.

#### Scenario: Security group dependency chain is linear
- **WHEN** security groups are created
- **THEN** dependency chain is: NLB SG (no deps) -> ECS SG (refs NLB) -> RDS SG (refs ECS)

#### Scenario: No security group references itself
- **WHEN** any security group is created
- **THEN** security group does not reference its own ID in ingress/egress rules
