## Why

The ECS service currently relies on a TCP target group health check, which can mark tasks healthy even when the Quarkus application is not ready to serve requests. Adding an application-level health check now will make deployments and runtime recovery more reliable by letting AWS verify both container reachability and app readiness.

## What Changes

- Add a public health endpoint in the Quarkus application that returns a successful response only when the service is ready to handle traffic.
- Update application security and configuration so the health endpoint stays reachable without authentication while the rest of the API remains protected.
- Change the ECS/NLB target group health check from TCP probing to HTTP probing against the application health endpoint.
- Document the Terraform and application updates needed to keep ECS task registration aligned with the new health check behavior.

## Capabilities

### New Capabilities
- `application-health-endpoint`: Expose an unauthenticated HTTP health endpoint for infrastructure and operational checks.

### Modified Capabilities
- `nlb-routing`: Change target group health checks from TCP reachability to HTTP checks against the application health endpoint.

## Impact

- Application code in `app/` for Quarkus health dependencies, endpoint exposure, and auth rules.
- Terraform ECS/NLB resources in `terraform/modules/ecs/` for target group health check protocol and matcher settings.
- Deployment behavior in ECS, because target registration and replacement will now depend on application readiness instead of open TCP port checks.
