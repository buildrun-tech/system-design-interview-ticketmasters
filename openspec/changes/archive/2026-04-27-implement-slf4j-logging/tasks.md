## 1. Auth Layer Logging

- [x] 1.1 Add `[Start]` / `[End]` / `[WARN]` structured logging to `AccessTokenService.getAccessToken` — log `grantType` on start, warn on unknown grant type before throwing
- [x] 1.2 Add `[Start]` / `[End]` / `[WARN]` structured logging to `PasswordGrantTokenStrategy.generateToken` — log `grantType` and `identifier` (username), warn with `reason: INVALID_CREDENTIALS` on `LoginException`
- [x] 1.3 Add `[Start]` / `[End]` / `[WARN]` structured logging to `ClientCredentialsTokenStrategy.generateToken` — log `grantType` and `identifier` (clientId UUID), warn with `reason: INVALID_CREDENTIALS` on `LoginException`

## 2. Booking State Transition Logging

- [x] 2.1 Add `[Start]` / `[End]` / `[WARN]` structured logging to `UpdateBookingStatusService.updateBookingStatus` — log `bookingId` and `status` on start; log `bookingId`, `previousStatus`, and `newStatus` on end; warn when status is not PENDING
- [x] 2.2 Add `[Start]` / `[End]` structured logging to `ExpireBookingService.expireBookings` — log `bookingId` on start; log `bookingId` and outcome (`EXPIRED` or `SKIPPED`) on end

## 3. Async SQS Pipeline Logging

- [x] 3.1 Add `[Start]` / `[End]` / `[ERROR]` structured logging to `BookingExpirationService.scheduleExpirationCheck` — log `bookingId` and `delaySeconds` on end
- [x] 3.2 Refactor `CheckBookingSqsListener.receive` to use `addKeyValue("bookingId", dto.bookingId())` instead of logging the raw `message` object; keep existing WARN/ERROR logic but convert to structured format

## 4. Business Operation Logging

- [x] 4.1 Add `[Start]` / `[End]` structured logging to `EventService.createEvent` — log `numberOfSeats` on start; log `eventId` and `eventName` on end
- [x] 4.2 Add `[Start]` / `[End]` structured logging to `EventService.findById` — log `eventId` on start; warn if not found
- [x] 4.3 Add `[Start]` / `[End]` / `[WARN]` structured logging to `UserService.createUser` — log `username` and `email` on start; warn on duplicate user before throwing
- [x] 4.4 Add `[Start]` / `[End]` / `[WARN]` structured logging to `AdminService.setupAdminUser` — log `username` on start; warn when admin already exists
- [x] 4.5 Add `[Start]` / `[End]` / `[WARN]` structured logging to `AppService.createApp` — log `appName` and `scopes` on start; log `appName` and `clientId` on end (NEVER log `clientSecret`); warn on duplicate name

## 5. Exception Mapper Logging

- [x] 5.1 Add logger to `TicketMasterExceptionMapper` and emit a WARN log with exception type and HTTP status in `toResponse`
- [x] 5.2 Add logger to `ConstraintViolationExceptionMapper` and emit a WARN log with the list of violated field names in `toResponse`
