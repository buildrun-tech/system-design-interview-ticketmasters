## ADDED Requirements

### Requirement: Application exposes a readiness health endpoint
The system SHALL expose an HTTP readiness endpoint that reports whether the Quarkus service is ready to receive production traffic.

#### Scenario: Readiness endpoint is available
- **WHEN** the application is running
- **THEN** the service exposes a readiness endpoint at `/q/health/ready`

#### Scenario: Ready application returns success
- **WHEN** the application has completed startup and its readiness checks pass
- **THEN** `GET /q/health/ready` returns an HTTP 200 response

#### Scenario: Unready application returns failure
- **WHEN** the application readiness checks fail
- **THEN** `GET /q/health/ready` returns a non-200 response that can be treated as unhealthy by infrastructure

### Requirement: Health endpoint is reachable without authentication
The system SHALL allow infrastructure callers to access the health endpoint without presenting JWT credentials.

#### Scenario: Load balancer can call readiness endpoint anonymously
- **WHEN** the Network Load Balancer performs an HTTP request to `/q/health/ready`
- **THEN** the request is not rejected for missing authentication credentials

#### Scenario: Existing API protection remains in place
- **WHEN** a caller accesses a non-public business endpoint without valid credentials
- **THEN** the application continues to require authentication according to the existing JWT policy
