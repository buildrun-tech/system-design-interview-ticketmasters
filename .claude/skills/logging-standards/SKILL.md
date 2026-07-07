---
name: logging-standards
description: 'Standardize application logging using SLF4j with [Start]/[End] patterns. Use when: adding logging to service methods, business logic, and critical operations. Includes code templates and best practices for non-sensitive parameter logging.'
argument-hint: 'Service/method name to add logging to'
---

# Logging Standards

## Overview

This skill provides a standardized approach to logging across the application using **SLF4j**. All logs follow a consistent pattern:
- **Start**: `[Start] Action name` at method entry
- **End**: `[End] Action name` at method exit
- Non-sensitive parameters logged throughout
- Clear tracing of operation flow

## When to Use

- Adding logging to **service/business logic methods**
- Logging **controller operations** and request handling
- Tracking **critical operations** (bookings, payments, user actions)
- Debugging **flow and state changes** in complex operations
- **Repository layer** operations (queries, updates, deletes)

## Core Principles

### 1. Non-Sensitive Data Only
✅ Log these:
- User IDs, booking IDs, event IDs, request IDs
- Operation names and action types
- Parameter names and their values (for non-sensitive data)
- Boolean flags, counts, result statuses
- Timing information, elapsed time

❌ Never log:
- Passwords, API keys, tokens, secrets
- Payment card details, financial information
- Personal identifiable information (full names, emails, addresses)
- Raw DTOs containing sensitive fields (extract safe fields first)

### 2. Naming Convention
Use descriptive action names that explain what the method does:
- ✅ `createBooking`, `confirmPayment`, `fetchEventSeats`
- ✅ `validateUserCredentials`, `generateConfirmationEmail`
- ❌ `process`, `execute`, `handle` (too vague)

### 3. Parameter Logging Strategy
Extract non-sensitive fields from DTOs before logging:

```java
// ❌ Bad: Don't log entire DTO
log.info("[Start] createUser with: {}", userDTO);

// ✅ Good: Extract safe fields
log.info("[Start] createUser - userId: {}, email: {}, role: {}", 
    userDTO.getId(), userDTO.getEmail(), userDTO.getRole());
```

## Code Templates

### Basic Service Method Template

[See template →](./assets/ServiceMethodTemplate.java)

```java
private static final Logger log = LoggerFactory.getLogger(YourClass.class);

public String createBooking(BookingRequest request) {
    log.info("[Start] createBooking - userId: {}, eventId: {}, seatCount: {}", 
        request.getUserId(), request.getEventId(), request.getSeatCount());
    
    try {
        // Business logic here
        String bookingId = generateBookingId();
        
        log.info("[End] createBooking - bookingId: {}, status: CREATED", bookingId);
        return bookingId;
    } catch (Exception e) {
        log.error("[Error] createBooking - userId: {}, eventId: {}, error: {}", 
            request.getUserId(), request.getEventId(), e.getMessage(), e);
        throw new BookingException("Failed to create booking", e);
    }
}
```

### Query/Repository Method Template

```java
public Event findEventById(Long eventId) {
    log.info("[Start] findEventById - eventId: {}", eventId);
    
    Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new EventNotFoundException(eventId));
    
    log.info("[End] findEventById - eventId: {}, title: {}, capacity: {}", 
        eventId, event.getTitle(), event.getCapacity());
    
    return event;
}
```

### Error Handling with Logging Template

```java
public boolean confirmBooking(String bookingId) {
    log.info("[Start] confirmBooking - bookingId: {}", bookingId);
    
    try {
        Booking booking = bookingRepository.findById(bookingId);
        validateBooking(booking);
        
        booking.setStatus("CONFIRMED");
        bookingRepository.save(booking);
        
        log.info("[End] confirmBooking - bookingId: {}, newStatus: CONFIRMED", bookingId);
        return true;
    } catch (ValidationException e) {
        log.warn("[End] confirmBooking - bookingId: {}, reason: {}", bookingId, e.getMessage());
        return false;
    } catch (Exception e) {
        log.error("[Error] confirmBooking - bookingId: {}, exception: {}", 
            bookingId, e.getMessage(), e);
        throw new BookingException("Failed to confirm booking", e);
    }
}
```

## Implementation Steps

1. **Add SLF4j logger** to your class:
   ```java
   private static final Logger log = LoggerFactory.getLogger(YourClass.class);
   ```

2. **Identify non-sensitive parameters** from method inputs
   - Extract DTOs into individual fields
   - Note: User IDs, request IDs, and operation names are safe to log

3. **Add [Start] log** at method entry with parameters:
   ```java
   log.info("[Start] methodName - param1: {}, param2: {}", param1, param2);
   ```

4. **Add operation-specific logs** for important state changes or checkpoints

5. **Add [End] log** at successful completion with result:
   ```java
   log.info("[End] methodName - resultId: {}, status: SUCCESS", resultId);
   ```

6. **Add error logs** in catch blocks with context:
   ```java
   log.error("[Error] methodName - context: {}, error: {}", context, e.getMessage(), e);
   ```

## Logging Levels

- **INFO**: Normal operation flow ([Start], [End], state changes)
- **WARN**: Validations failed, non-fatal issues ([End] with reason)
- **ERROR**: Exceptions, critical failures ([Error], include full exception)
- **DEBUG**: Detailed debugging info (intermediate calculations, state snapshots)

## Examples in Codebase

Check existing implementations for reference patterns:
- Service methods in `src/main/java/tech/buildrun/service/`
- Repository methods in `src/main/java/tech/buildrun/repository/`

## Common Patterns

### Conditional Logging
```java
log.info("[Start] processEvent - eventId: {}, async: {}", eventId, isAsync);
```

### Multiple Operations
```java
log.info("[Start] bulkCreateEvents - count: {}", events.size());
// ... process each
log.info("[End] bulkCreateEvents - successful: {}, failed: {}", success, failed);
```

### State Transitions
```java
log.info("[Start] updateUserRole - userId: {}, oldRole: {}, newRole: {}", 
    userId, oldRole, newRole);
// ... update logic
log.info("[End] updateUserRole - userId: {}, newRole: {}", userId, newRole);
```

## Quick Reference

| Scenario | Template |
|----------|----------|
| Method entry | `log.info("[Start] methodName - ...params")` |
| Method exit (success) | `log.info("[End] methodName - ...results")` |
| Validation failed | `log.warn("[End] methodName - reason: ...")` |
| Exception caught | `log.error("[Error] methodName - context, exception, e)")` |
| Debug info | `log.debug("[Debug] methodName - ...details")` |

---

**Location**: `.claude/skills/logging-standards/`  
**References**: [ServiceMethodTemplate.java](./assets/ServiceMethodTemplate.java)
