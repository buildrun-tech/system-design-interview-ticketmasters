## Context

The current deployment exposes the Quarkus service behind an internal Network Load Balancer and uses a TCP target group health check on the application port. That confirms only that the container accepts a socket connection; it does not verify that Quarkus finished booting, the datasource is reachable, or the app can actually serve traffic. The application also protects nearly every route with JWT auth, so any infrastructure-facing health endpoint must be explicitly left public.

## Goals / Non-Goals

**Goals:**
- Add an application-level health endpoint that infrastructure can call without credentials.
- Make ECS target registration depend on HTTP readiness instead of bare TCP reachability.
- Keep the change small and aligned with existing Quarkus and Terraform patterns.

**Non-Goals:**
- Rework API Gateway integrations or add external monitoring systems.
- Introduce deep synthetic checks for downstream dependencies beyond what Quarkus readiness already exposes.
- Change business endpoints or JWT behavior outside the health route exception.

## Decisions

### Use Quarkus SmallRye Health instead of a custom controller
- Decision: add the `quarkus-smallrye-health` extension and use Quarkus health endpoints.
- Rationale: this gives standardized liveness/readiness endpoints, integrates cleanly with Quarkus startup state, and can automatically include datasource readiness without bespoke controller logic.
- Alternative considered: a custom `/health` REST endpoint. Rejected because it would duplicate framework behavior and make readiness semantics easier to get wrong.

### Probe readiness over HTTP from the target group
- Decision: update the NLB target group health check to use `HTTP` against `/q/health/ready` on the traffic port.
- Rationale: readiness reflects whether the app is actually ready to receive traffic, which is the condition ECS deployments need.
- Alternative considered: keep TCP checks. Rejected because TCP can succeed while the app is still starting or partially degraded.

### Keep the health route public while preserving existing API auth
- Decision: expand the Quarkus public permission paths to include `/q/health/*` and leave the existing catch-all authenticated rule in place.
- Rationale: the load balancer cannot present JWT credentials, so the health route must be reachable anonymously while all other business routes stay protected.
- Alternative considered: a separate listener or port for health checks. Rejected because it adds infrastructure complexity for little benefit.

## Risks / Trade-offs

- [Framework readiness becomes stricter than TCP reachability] -> Mitigation: use the standard readiness endpoint so unhealthy tasks fail fast and ECS can replace them correctly.
- [Health endpoint path changes could break load balancer checks] -> Mitigation: document the exact path in specs and keep Terraform and application changes in the same implementation task.
- [Readiness may fail when the database is unavailable] -> Mitigation: accept this trade-off because serving traffic without critical dependencies is worse than marking the task unhealthy.

## Migration Plan

- Add the Quarkus health dependency and public auth exception for `/q/health/*`.
- Verify the app exposes readiness successfully in local or test execution.
- Update the NLB target group health check to use HTTP and the readiness path.
- Apply Terraform so new target group settings roll out with ECS service registration.
- Roll back by reverting the Terraform health check to TCP and removing the health dependency/path exception if the rollout exposes unexpected readiness failures.

## Open Questions

- None at proposal time; the implementation can use Quarkus defaults unless environment-specific health behavior proves different during verification.
