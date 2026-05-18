# rds-cloudshell-vpc-access Specification

## Purpose
Define RDS security group rules that allow PostgreSQL access from the VPC CIDR and the VPC default security group, enabling CloudShell VPC connections to the database for operational access.

## Requirements

### Requirement: RDS security group accepts TCP 5432 from VPC CIDR
O sistema SHALL adicionar uma regra inbound ao security group do RDS que aceita conexões TCP na porta 5432 originadas de qualquer endereço dentro do CIDR da VPC.

#### Scenario: RDS SG allows VPC CIDR on port 5432
- **WHEN** RDS security group is created or updated
- **THEN** an ingress rule allows TCP port 5432 from `data.aws_vpc.existing.cidr_block`

#### Scenario: RDS SG VPC CIDR rule has descriptive description
- **WHEN** RDS security group VPC CIDR ingress rule is created
- **THEN** ingress rule description is "PostgreSQL from VPC CIDR" or similar

### Requirement: RDS security group accepts TCP 5432 from VPC default security group
O sistema SHALL adicionar uma regra inbound ao security group do RDS que aceita conexões TCP na porta 5432 origindas de recursos associados ao security group default da VPC (incluindo ENIs do CloudShell VPC).

#### Scenario: RDS SG allows default SG on port 5432
- **WHEN** RDS security group is created or updated
- **THEN** an ingress rule allows TCP port 5432 from `data.aws_security_group.default.id`

#### Scenario: Default SG is resolved via data source
- **WHEN** networking module is applied
- **THEN** the default SG ID is resolved via `data "aws_security_group" "default"` with `name = "default"` and `vpc_id = var.vpc_id`

#### Scenario: RDS SG default SG rule has descriptive description
- **WHEN** RDS security group default SG ingress rule is created
- **THEN** ingress rule description is "PostgreSQL from default SG" or "Access from VPC" or similar
