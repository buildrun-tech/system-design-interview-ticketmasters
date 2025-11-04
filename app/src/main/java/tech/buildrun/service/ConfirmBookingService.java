package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tech.buildrun.entity.BookingEntity;
import tech.buildrun.entity.BookingStatus;
import tech.buildrun.exception.ResourceNotFoundException;

@ApplicationScoped
public class ConfirmBookingService {


    @Transactional
    public void confirmBooking(Long bookingId) {

        var entity = BookingEntity.findByIdOptional(bookingId)
                .map(BookingEntity.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "Booking with id not found"));

        entity.status = BookingStatus.CONFIRMED;
        entity.persist();
    }
}
