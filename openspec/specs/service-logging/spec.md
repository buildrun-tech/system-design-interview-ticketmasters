# service-logging Specification

## Purpose
Define structured SLF4J logging standards for service methods, including Start/End bracketing, exception handling levels, and domain-specific logging rules for auth, booking, and SQS pipeline operations.

## Requirements

### Requirement: Every public service method SHALL emit structured Start and End log entries

Each public method in a service class SHALL log a `[Start]` entry on entry with non-sensitive input parameters, and a `[End]` entry on successful completion with relevant output context. Both entries SHALL use the SLF4J fluent API with `addKeyValue` for each contextual field.

#### Scenario: Normal method execution is fully bracketed
- **WHEN** a public service method is called with valid inputs
- **THEN** a `[Start]` INFO log entry is emitted before any business logic executes
- **THEN** a `[End]` INFO log entry is emitted after successful completion
- **THEN** both entries contain the same request-scoped key-value pairs (e.g., `userId`, `bookingId`)

#### Scenario: End log includes output context
- **WHEN** a service method creates or updates an entity
- **THEN** the `[End]` log entry SHALL include the generated or updated identifier (e.g., `bookingId`, `eventId`)

---

### Requirement: Exceptions SHALL be logged at the appropriate level before re-throwing

When a service method catches an exception, it SHALL log the exception at `WARN` level for expected business failures or `ERROR` level for unexpected system failures, then re-throw the exception unchanged.

#### Scenario: Expected business exception is logged as WARN
- **WHEN** a service method throws `LoginException`, `SeatAlreadyBookedException`, `ResourceNotFoundException`, or `UpdateBookingException`
- **THEN** the exception is logged at `WARN` level with relevant context key-values and the exception message
- **THEN** the original exception is re-thrown without wrapping

#### Scenario: Unexpected exception is logged as ERROR with stack trace
- **WHEN** a service method encounters an unexpected `Exception` not in the expected-exception list
- **THEN** the exception is logged at `ERROR` level with context key-values and the full stack trace as the last argument to the log call
- **THEN** the original exception is re-thrown

---

### Requirement: Auth service methods SHALL log grant type and identity, never credentials

`AccessTokenService`, `PasswordGrantTokenStrategy`, and `ClientCredentialsTokenStrategy` SHALL log the `grantType` and sanitized identity (`identifier` — username or clientId UUID) on every token generation attempt. Passwords, client secrets, and JWT tokens SHALL NEVER appear in any log entry.

#### Scenario: Successful token generation is logged
- **WHEN** a token is successfully generated for any grant type
- **THEN** a `[End]` INFO log entry is emitted with `grantType` and `identifier` fields
- **THEN** the log entry MUST NOT contain the credential (password or clientSecret)
- **THEN** the log entry MUST NOT contain the generated JWT token value

#### Scenario: Failed login attempt is logged as WARN
- **WHEN** `PasswordGrantTokenStrategy` or `ClientCredentialsTokenStrategy` throws `LoginException`
- **THEN** a WARN log entry is emitted with `grantType` and `identifier` before re-throwing
- **THEN** the reason field SHALL be `INVALID_CREDENTIALS`

#### Scenario: Invalid grant type request is logged as WARN
- **WHEN** `AccessTokenService.getAccessToken` receives an unknown `grantType`
- **THEN** a WARN log entry is emitted with the attempted `grantType` value before throwing

---

### Requirement: Booking state transitions SHALL be logged with before/after status

`UpdateBookingStatusService` and `ExpireBookingService` SHALL log the `bookingId` and the status transition (`previousStatus → newStatus`) at INFO level on every state change.

#### Scenario: Booking status update is logged with transition
- **WHEN** `UpdateBookingStatusService.updateBookingStatus` is called
- **THEN** the `[Start]` log SHALL include `bookingId` and requested `status`
- **THEN** the `[End]` log SHALL include `bookingId`, `previousStatus`, and `newStatus`

#### Scenario: Booking expiration with seat release is logged
- **WHEN** `ExpireBookingService.expireBookings` transitions a booking to EXPIRED
- **THEN** the `[End]` log SHALL include `bookingId` and `status: EXPIRED`
- **WHEN** `ExpireBookingService.expireBookings` finds a non-PENDING booking
- **THEN** a `[End]` log SHALL indicate `status: SKIPPED` (no transition needed)

---

### Requirement: SQS pipeline steps SHALL be logged with bookingId and delaySeconds

`BookingExpirationService.scheduleExpirationCheck` SHALL log the `bookingId` and `delaySeconds` at INFO level. `CheckBookingSqsListener` SHALL log using structured key-value pairs, not raw message object interpolation.

#### Scenario: SQS message is sent with structured log
- **WHEN** `BookingExpirationService.scheduleExpirationCheck` sends a delayed message
- **THEN** a `[End]` INFO log entry is emitted with `bookingId` and `delaySeconds`
- **THEN** the log entry SHALL NOT include the raw `SendMessageRequest` object

#### Scenario: SQS listener logs bookingId, not raw message
- **WHEN** `CheckBookingSqsListener.receive` processes a message
- **THEN** the log entries SHALL use `addKeyValue("bookingId", dto.bookingId())` after deserialization
- **THEN** the raw `Message<String>` object SHALL NOT be passed to any log statement

---

### Requirement: Exception mappers SHALL log intercepted exceptions before responding

`TicketMasterExceptionMapper` SHALL log at `WARN` level for all `TicketMasterException` instances. `ConstraintViolationExceptionMapper` SHALL log at `WARN` level with the list of violated field names.

#### Scenario: Domain exception is logged before HTTP response
- **WHEN** `TicketMasterExceptionMapper.toResponse` is invoked
- **THEN** a WARN log entry is emitted with the exception type and HTTP status
- **THEN** the HTTP response is still returned normally

#### Scenario: Constraint violation is logged with field names
- **WHEN** `ConstraintViolationExceptionMapper.toResponse` is invoked
- **THEN** a WARN log entry is emitted with the list of violated parameter names
