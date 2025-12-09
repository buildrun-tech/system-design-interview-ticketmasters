package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.buildrun.entity.BookingEntity;
import tech.buildrun.entity.BookingStatus;
import tech.buildrun.exception.ResourceNotFoundException;
import tech.buildrun.exception.UpdateBookingException;

@ApplicationScoped
public class UpdateBookingStatusService {


    @Transactional
    public void updateBookingStatus(Long bookingId, BookingStatus status) {

        var entity = BookingEntity.findByIdOptional(bookingId)
                .map(BookingEntity.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "Booking with id not found"));

        if (!entity.status.equals(BookingStatus.PENDING)) {
            throw new UpdateBookingException("Booking status cannot be updated",
                    "Booking with id " + bookingId + " is not in PENDING status");
        }

        entity.status = status;
        entity.persist();
    }
}
