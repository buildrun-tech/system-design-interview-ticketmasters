## Why

The application lacks consistent observability: 9 of 11 service classes have zero logging, auth flows give no visibility into login failures, and critical booking state transitions (PENDING → EXPIRED/CONFIRMED) leave no trace. Without structured logs, debugging incidents in production or detecting security threats is impractical.

## What Changes

- Add SLF4J structured logging (fluent API with `addKeyValue`) to all service classes following the established pattern in `BookingService`
- Log `[Start]` / `[End]` / `[Error]` markers on every public service method
- Add `[Error]` logging to exception mappers so HTTP errors are visible server-side
- Standardize `CheckBookingSqsListener` to use structured key-value logging instead of raw string interpolation
- Never log sensitive fields: passwords, clientSecrets, JWT tokens, or bcrypt hashes

## Capabilities

### New Capabilities
- `service-logging`: Structured SLF4J logging across all service classes and the SQS listener using the fluent `addKeyValue` API

### Modified Capabilities
<!-- None — this adds logging as a cross-cutting concern without changing any functional requirements -->

## Impact

- **Service classes affected**: `UpdateBookingStatusService`, `ExpireBookingService`, `BookingExpirationService`, `EventService`, `UserService`, `AccessTokenService`, `AdminService`, `AppService`, `PasswordGrantTokenStrategy`, `ClientCredentialsTokenStrategy`
- **Listener affected**: `CheckBookingSqsListener` (refactor existing logs to structured format)
- **Exception mappers affected**: `TicketMasterExceptionMapper`, `ConstraintViolationExceptionMapper`
- **No API changes** — purely internal observability improvement
- **No new dependencies** — SLF4J is already on the classpath (used by `BookingService`)
