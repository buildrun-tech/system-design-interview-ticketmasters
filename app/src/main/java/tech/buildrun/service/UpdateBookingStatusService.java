package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.entity.BookingEntity;
import tech.buildrun.entity.BookingStatus;
import tech.buildrun.exception.ResourceNotFoundException;
import tech.buildrun.exception.UpdateBookingException;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class UpdateBookingStatusService {

    private static final Logger logger = getLogger(UpdateBookingStatusService.class);

    @Transactional
    public void updateBookingStatus(Long bookingId, BookingStatus status) {

        logger.atInfo()
                .addKeyValue("bookingId", bookingId)
                .addKeyValue("status", status)
                .log("[Start] updateBookingStatus");

        var entity = BookingEntity.findByIdOptional(bookingId)
                .map(BookingEntity.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "Booking with id not found"));

        if (!entity.status.equals(BookingStatus.PENDING)) {
            logger.atWarn()
                    .addKeyValue("bookingId", bookingId)
                    .addKeyValue("currentStatus", entity.status)
                    .addKeyValue("reason", "NOT_PENDING")
                    .log("[End] updateBookingStatus");
            throw new UpdateBookingException("Booking status cannot be updated",
                    "Booking with id " + bookingId + " is not in PENDING status");
        }

        var previousStatus = entity.status;
        entity.status = status;
        entity.persist();

        logger.atInfo()
                .addKeyValue("bookingId", bookingId)
                .addKeyValue("previousStatus", previousStatus)
                .addKeyValue("newStatus", status)
                .log("[End] updateBookingStatus");
    }
}
