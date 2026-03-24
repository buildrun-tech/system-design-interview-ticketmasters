## ADDED Requirements

### Requirement: ECS security group preserves restricted inbound access with public-IP egress
The system SHALL keep ECS task inbound access restricted to the NLB security group even when ECS tasks receive public IP addresses for outbound internet connectivity.

#### Scenario: ECS inbound rule still references the NLB security group
- **WHEN** the ECS security group ingress rules are rendered
- **THEN** inbound application traffic is allowed from `aws_security_group.nlb.id` on the container port

#### Scenario: ECS tasks are not opened directly to the internet
- **WHEN** the ECS security group ingress rules are evaluated
- **THEN** access is not granted from `0.0.0.0/0` or the full VPC CIDR block

### Requirement: ECS security group remains compatible with internet-bound bootstrap traffic
The system SHALL preserve an outbound path from ECS tasks to public AWS service endpoints so bootstrap calls such as Secrets Manager retrieval can complete.

#### Scenario: ECS tasks can initiate HTTPS traffic to public AWS endpoints
- **WHEN** ECS tasks attempt to open HTTPS connections to public AWS service endpoints
- **THEN** the ECS security group permits the outbound connection
