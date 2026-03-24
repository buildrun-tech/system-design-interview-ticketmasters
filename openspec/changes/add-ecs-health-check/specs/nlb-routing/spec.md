## MODIFIED Requirements

### Requirement: Health checks use TCP protocol
The system SHALL configure target group health checks using HTTP on the traffic port and probe the application readiness endpoint to validate target availability.

#### Scenario: Health check protocol is HTTP
- **WHEN** target group health check is configured
- **THEN** health check protocol is `HTTP`

#### Scenario: Health check uses traffic port
- **WHEN** target group health check is configured
- **THEN** health check port is `traffic-port` (8080)

#### Scenario: Health check path targets readiness endpoint
- **WHEN** target group health check is configured
- **THEN** health check path is `/q/health/ready`

#### Scenario: Health check accepts successful readiness response
- **WHEN** the target group evaluates a healthy target
- **THEN** the matcher accepts HTTP 200 from the readiness endpoint

#### Scenario: Health check thresholds are configured
- **WHEN** target group health check is configured
- **THEN** healthy threshold is 3 and unhealthy threshold is 3

#### Scenario: Health check interval is set
- **WHEN** target group health check is configured
- **THEN** health check interval is 30 seconds
