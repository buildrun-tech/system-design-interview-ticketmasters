## Context

The application is a Quarkus-based ticketing system. `BookingService` already establishes the logging pattern — SLF4J fluent API with `addKeyValue` for structured key-value pairs. The remaining 10 service classes and 2 exception mappers have no logging at all. `CheckBookingSqsListener` has basic logging but uses string interpolation and logs raw message objects instead of extracted payloads.

The fluent API (`logger.atInfo().addKeyValue("key", value).log("message")`) is preferred over string interpolation because it emits structured JSON-compatible log records that can be queried by field in CloudWatch Logs Insights or Datadog without log parsing.

## Goals / Non-Goals

**Goals:**
- Uniform `[Start]` / `[End]` / `[Error]` markers on every public service method
- Structured key-value pairs for all contextual fields
- Visibility into auth flows (login success/failure, grant type)
- Visibility into booking state transitions
- `WARN`-level logging on business exceptions (4xx) and `ERROR`-level on unexpected exceptions (5xx)

**Non-Goals:**
- Request/response body logging at the controller layer
- Distributed tracing or correlation IDs (separate concern)
- Log shipping or aggregation configuration
- Performance metrics or MDC thread-local context

## Decisions

### Decision 1: Use `logger.atInfo().addKeyValue()` everywhere (not string interpolation)

`BookingService` already uses the fluent API. We extend that pattern to all classes rather than mixing styles. The fluent API produces structured log records; string interpolation produces flat strings that require regex parsing downstream.

Alternative considered: stay with `logger.info("msg {}", param)` for simplicity — rejected because it breaks field-level querying in log aggregators.

### Decision 2: `WARN` for expected business failures, `ERROR` for unexpected

| Exception type | Log level | Rationale |
|---|---|---|
| `LoginException` (wrong password) | `WARN` | Expected, but worth alerting on volume |
| `SeatAlreadyBookedException` | `WARN` | Normal race condition |
| `ResourceNotFoundException` | `WARN` | Client error, not a system fault |
| `UpdateBookingException` | `WARN` | Invalid state transition requested |
| Unexpected `Exception` | `ERROR` | System fault, needs investigation |

### Decision 3: Log at service layer, not controller layer

Controllers are thin (they delegate directly to services). Logging at the service layer captures business intent. Controller logging would duplicate context without adding value.

Exception: exception mappers are JAX-RS infrastructure — they get a single log line at the appropriate level when intercepting an exception.

### Decision 4: Standardize field names across all services

Consistent field names enable cross-service aggregation queries:

| Field | Type | Used in |
|---|---|---|
| `userId` | Long | BookingService, UserService |
| `bookingId` | Long | UpdateBookingStatusService, ExpireBookingService, BookingExpirationService |
| `eventId` | Long | EventService |
| `seats` | List<Long> | BookingService |
| `status` | String | UpdateBookingStatusService, ExpireBookingService |
| `previousStatus` | String | UpdateBookingStatusService |
| `grantType` | String | AccessTokenService |
| `identifier` | String | TokenStrategy (username or clientId — never the secret) |
| `appName` | String | AppService |
| `clientId` | UUID | AppService, ClientCredentialsTokenStrategy |
| `scopes` | Set<String> | AppService |
| `numberOfSeats` | int | EventService |

### Decision 5: Never log sensitive fields

Fields that MUST NOT appear in any log statement:
- `password`, `secret`, `clientSecret` — credentials
- `accessToken` — JWT bearer token
- The full DTO object in auth flows (contains secret fields)

## Risks / Trade-offs

- [Risk] Log volume increase may raise CloudWatch costs → Mitigation: only INFO+ in production; DEBUG is off by default in Quarkus
- [Risk] A developer accidentally logs a sensitive field in a future PR → Mitigation: the spec explicitly lists forbidden fields; code review gate
- [Risk] `CheckBookingSqsListener` refactor could change log format that existing alerts depend on → Mitigation: no production alerts are known to depend on the current format; low risk

## Migration Plan

1. Implement logging in each service class in a single PR
2. No rollback needed — logging additions are additive and non-breaking
3. No database migrations, API changes, or config changes required
