package tech.buildrun.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template demonstrating standardized logging in service methods.
 * 
 * Key practices:
 * - Log [Start] with non-sensitive input parameters
 * - Log [End] with operation results
 * - Log [Error] with exception context
 * - Never log passwords, tokens, or sensitive PII
 */
public class ServiceMethodTemplate {
    private static final Logger log = LoggerFactory.getLogger(ServiceMethodTemplate.class);

    // ========== BASIC SERVICE METHOD ==========
    /**
     * Simple service method with start/end logging pattern.
     */
    public String createBooking(BookingRequest request) {
        log.info("[Start] createBooking - userId: {}, eventId: {}, seatCount: {}", 
            request.getUserId(), request.getEventId(), request.getSeatCount());
        
        try {
            // Validate input
            validateBookingRequest(request);
            
            // Business logic
            String bookingId = generateBookingId();
            Booking booking = new Booking(bookingId, request.getUserId(), request.getEventId());
            
            // Save to database
            bookingRepository.save(booking);
            
            log.info("[End] createBooking - bookingId: {}, userId: {}, status: CREATED", 
                bookingId, request.getUserId());
            
            return bookingId;
        } catch (ValidationException e) {
            log.warn("[End] createBooking - userId: {}, reason: VALIDATION_FAILED, message: {}", 
                request.getUserId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Error] createBooking - userId: {}, eventId: {}, error: {}", 
                request.getUserId(), request.getEventId(), e.getMessage(), e);
            throw new BookingException("Failed to create booking", e);
        }
    }

    // ========== QUERY METHOD ==========
    /**
     * Repository/query method with logging.
     */
    public Event findEventById(Long eventId) {
        log.info("[Start] findEventById - eventId: {}", eventId);
        
        try {
            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));
            
            log.info("[End] findEventById - eventId: {}, title: {}, capacity: {}", 
                eventId, event.getTitle(), event.getCapacity());
            
            return event;
        } catch (EventNotFoundException e) {
            log.warn("[End] findEventById - eventId: {}, reason: NOT_FOUND", eventId);
            throw e;
        } catch (Exception e) {
            log.error("[Error] findEventById - eventId: {}, error: {}", 
                eventId, e.getMessage(), e);
            throw e;
        }
    }

    // ========== UPDATE METHOD WITH STATE TRANSITION ==========
    /**
     * Method showing state transitions and logging intermediate steps.
     */
    public void confirmBooking(String bookingId) {
        log.info("[Start] confirmBooking - bookingId: {}", bookingId);
        
        try {
            Booking booking = bookingRepository.findById(bookingId);
            String previousStatus = booking.getStatus();
            
            // Validate current state
            if (!previousStatus.equals("PENDING")) {
                log.warn("[End] confirmBooking - bookingId: {}, reason: INVALID_STATUS, currentStatus: {}", 
                    bookingId, previousStatus);
                throw new InvalidStateException("Cannot confirm booking in status: " + previousStatus);
            }
            
            // Update status
            booking.setStatus("CONFIRMED");
            booking.setConfirmedAt(System.currentTimeMillis());
            bookingRepository.save(booking);
            
            log.info("[End] confirmBooking - bookingId: {}, statusTransition: {} -> CONFIRMED", 
                bookingId, previousStatus);
        } catch (InvalidStateException e) {
            log.warn("[End] confirmBooking - bookingId: {}, reason: {}", 
                bookingId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Error] confirmBooking - bookingId: {}, error: {}", 
                bookingId, e.getMessage(), e);
            throw new BookingException("Failed to confirm booking", e);
        }
    }

    // ========== BULK OPERATION ==========
    /**
     * Method performing bulk operations with progress logging.
     */
    public BulkResult createEvents(List<EventCreateRequest> events) {
        log.info("[Start] createEvents - eventCount: {}", events.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        try {
            for (EventCreateRequest eventRequest : events) {
                try {
                    String eventId = createSingleEvent(eventRequest);
                    successCount++;
                    log.debug("[Progress] createEvents - processed eventId: {}", eventId);
                } catch (Exception e) {
                    failureCount++;
                    log.warn("[Progress] createEvents - failed to create event, reason: {}", 
                        e.getMessage());
                }
            }
            
            log.info("[End] createEvents - successful: {}, failed: {}", successCount, failureCount);
            return new BulkResult(successCount, failureCount);
        } catch (Exception e) {
            log.error("[Error] createEvents - total: {}, processed: {}, error: {}", 
                events.size(), (successCount + failureCount), e.getMessage(), e);
            throw e;
        }
    }

    // ========== METHOD WITH DTO SAFE FIELD EXTRACTION ==========
    /**
     * Demonstrates extracting safe fields from DTOs before logging.
     * IMPORTANT: Do NOT log entire DTOs - extract safe fields only.
     */
    public void processUserRegistration(UserRegistrationDTO userDTO) {
        // ✅ CORRECT: Extract only safe fields
        log.info("[Start] processUserRegistration - userId: {}, email: {}, role: {}", 
            userDTO.getId(), userDTO.getEmail(), userDTO.getRole());
        
        // ❌ WRONG: Never do this - logs passwords and sensitive data
        // log.info("[Start] processUserRegistration - user: {}", userDTO);
        
        try {
            validateUserData(userDTO);
            
            // Business logic
            User user = new User(userDTO.getId(), userDTO.getEmail(), userDTO.getRole());
            userRepository.save(user);
            
            log.info("[End] processUserRegistration - userId: {}, role: {}, status: CREATED", 
                userDTO.getId(), userDTO.getRole());
        } catch (ValidationException e) {
            log.warn("[End] processUserRegistration - userId: {}, reason: VALIDATION_FAILED", 
                userDTO.getId());
            throw e;
        } catch (Exception e) {
            log.error("[Error] processUserRegistration - userId: {}, error: {}", 
                userDTO.getId(), e.getMessage(), e);
            throw new RegistrationException("Failed to register user", e);
        }
    }

    // ========== CONDITIONAL LOGGING ==========
    /**
     * Example with conditional logic and different execution paths.
     */
    public PaymentResult processPayment(String bookingId, boolean useNewProvider) {
        log.info("[Start] processPayment - bookingId: {}, useNewProvider: {}", 
            bookingId, useNewProvider);
        
        try {
            Booking booking = bookingRepository.findById(bookingId);
            
            PaymentResult result;
            if (useNewProvider) {
                log.info("[Progress] processPayment - using NEW payment provider");
                result = newPaymentGateway.process(booking.getPrice());
            } else {
                log.info("[Progress] processPayment - using LEGACY payment provider");
                result = legacyPaymentGateway.process(booking.getPrice());
            }
            
            if (result.isSuccessful()) {
                booking.setPaymentStatus("COMPLETED");
                booking.setPaymentId(result.getPaymentId());
                bookingRepository.save(booking);
                
                log.info("[End] processPayment - bookingId: {}, paymentId: {}, status: SUCCESS", 
                    bookingId, result.getPaymentId());
            } else {
                log.warn("[End] processPayment - bookingId: {}, reason: {}", 
                    bookingId, result.getFailureReason());
            }
            
            return result;
        } catch (Exception e) {
            log.error("[Error] processPayment - bookingId: {}, error: {}", 
                bookingId, e.getMessage(), e);
            throw new PaymentException("Payment processing failed", e);
        }
    }

    // ========== HELPER METHODS (Shown for context, don't log in helpers) ==========
    private String generateBookingId() {
        return UUID.randomUUID().toString();
    }

    private void validateBookingRequest(BookingRequest request) {
        // Validation logic
    }

    private String createSingleEvent(EventCreateRequest eventRequest) {
        // Single event creation logic
        return "eventId";
    }

    private void validateUserData(UserRegistrationDTO userDTO) {
        // Validation logic
    }
}
