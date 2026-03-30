## ADDED Requirements

### Requirement: NLB is internal only
The system SHALL create a Network Load Balancer with `internal = true` to prevent public internet access and enable private VPC Link integration.

#### Scenario: NLB is not internet-facing
- **WHEN** NLB is created
- **THEN** NLB scheme is "internal"

#### Scenario: NLB has no public IP
- **WHEN** NLB is created
- **THEN** NLB does not have public IP addresses

### Requirement: NLB listens on port 8080 with TCP protocol
The system SHALL configure the NLB listener to accept TCP traffic on port 8080 to match the ECS container port.

#### Scenario: NLB listener accepts TCP on 8080
- **WHEN** NLB listener is created
- **THEN** listener protocol is "TCP" and port is 8080

### Requirement: Target group uses IP target type
The system SHALL configure the NLB target group with `target_type = "ip"` to support ECS Fargate tasks with awsvpc network mode.

#### Scenario: Target group accepts IP targets
- **WHEN** target group is created
- **THEN** target type is "ip"

#### Scenario: Target group port matches container port
- **WHEN** target group is created
- **THEN** target group port is 8080

#### Scenario: Target group protocol is TCP
- **WHEN** target group is created
- **THEN** target group protocol is "TCP"

### Requirement: ECS task IPs register automatically
The system SHALL configure the ECS service to automatically register and deregister task IP addresses with the NLB target group.

#### Scenario: ECS service references target group
- **WHEN** ECS service is created
- **THEN** ECS service includes load balancer configuration with target group ARN

#### Scenario: New tasks register with target group
- **WHEN** ECS starts a new task
- **THEN** task IP is automatically added to NLB target group

#### Scenario: Stopped tasks deregister from target group
- **WHEN** ECS stops a task
- **THEN** task IP is automatically removed from NLB target group

### Requirement: Health checks use HTTP readiness probing
The system SHALL configure target group health checks using HTTP on the traffic port and probe `/q/health/ready` to validate target readiness.

#### Scenario: Health check protocol is HTTP
- **WHEN** target group health check is configured
- **THEN** health check protocol is "HTTP"

#### Scenario: Health check uses traffic port
- **WHEN** target group health check is configured
- **THEN** health check port is "traffic-port" (8080)

#### Scenario: Health check path targets readiness endpoint
- **WHEN** target group health check is configured
- **THEN** health check path is `/q/health/ready`

#### Scenario: Health check matcher expects HTTP 200
- **WHEN** target group health check is configured
- **THEN** health check matcher is `200`

#### Scenario: Health check thresholds are configured
- **WHEN** target group health check is configured
- **THEN** healthy threshold is 3 and unhealthy threshold is 3

#### Scenario: Health check interval is set
- **WHEN** target group health check is configured
- **THEN** health check interval is 30 seconds

### Requirement: NLB enables cross-zone load balancing
The system SHALL enable cross-zone load balancing on the NLB to distribute traffic evenly across all availability zones.

#### Scenario: Cross-zone load balancing is enabled
- **WHEN** NLB is created
- **THEN** `enable_cross_zone_load_balancing` is true

### Requirement: NLB is deployed in private subnets
The system SHALL deploy the NLB in private subnets across multiple availability zones for high availability.

#### Scenario: NLB is in private subnets
- **WHEN** NLB is created
- **THEN** NLB subnet IDs match `var.private_subnet_ids`

#### Scenario: NLB spans multiple AZs
- **WHEN** NLB is created
- **THEN** NLB is deployed in at least 2 availability zones

### Requirement: NLB uses NLB security group
The system SHALL attach the NLB security group to the Network Load Balancer to control inbound traffic.

#### Scenario: NLB has security group attached
- **WHEN** NLB is created
- **THEN** NLB security groups include `var.nlb_security_group_id`

### Requirement: Deregistration delay allows graceful shutdown
The system SHALL configure a 30-second deregistration delay to allow in-flight requests to complete before removing targets.

#### Scenario: Deregistration delay is configured
- **WHEN** target group is created
- **THEN** deregistration delay is 30 seconds

### Requirement: NLB outputs are exposed
The system SHALL expose NLB DNS name and ARN as Terraform outputs for reference by API Gateway and monitoring systems.

#### Scenario: NLB DNS name is available
- **WHEN** Terraform apply completes
- **THEN** output `nlb_dns_name` contains the NLB DNS endpoint

#### Scenario: NLB ARN is available
- **WHEN** Terraform apply completes
- **THEN** output `nlb_arn` contains the NLB ARN

### Requirement: NLB has resource tags
The system SHALL tag the NLB and target group with common tags and descriptive names for resource management.

#### Scenario: NLB has tags
- **WHEN** NLB is created
- **THEN** NLB includes `var.common_tags` and Name tag

#### Scenario: Target group has tags
- **WHEN** target group is created
- **THEN** target group includes `var.common_tags` and Name tag
