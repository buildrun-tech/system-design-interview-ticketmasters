## 1. Application health endpoint

- [ ] 1.1 Add the Quarkus health extension dependency in `app/pom.xml`.
- [ ] 1.2 Update application auth/configuration so `/q/health/*` is publicly reachable while existing protected routes remain authenticated.
- [ ] 1.3 Verify the app exposes `/q/health/ready` with a healthy response when startup and readiness checks pass.

## 2. Terraform load balancer health checks

- [ ] 2.1 Update `terraform/modules/ecs/main.tf` so the target group health check uses HTTP on `traffic-port` with path `/q/health/ready`.
- [ ] 2.2 Configure the target group matcher and keep existing thresholds/intervals aligned with the spec.
- [ ] 2.3 Review Terraform inputs/outputs for any related changes and keep the ECS service registration behavior unchanged apart from the new health probe.

## 3. Validation

- [ ] 3.1 Run the relevant application test/build command to confirm the health endpoint configuration compiles and starts correctly.
- [ ] 3.2 Run the relevant Terraform validation command to confirm the ECS module accepts the updated health check configuration.
