package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.entity.*;
import tech.buildrun.exception.ResourceNotFoundException;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class ExpireBookingService {

    private static final Logger logger = getLogger(ExpireBookingService.class);

    @Transactional
    public void expireBookings(Long bookingId) {

        logger.atInfo()
                .addKeyValue("bookingId", bookingId)
                .log("[Start] expireBookings");

        var booking = findBooking(bookingId);

        expireBooking(booking);
    }

    private static BookingEntity findBooking(Long bookingId) {
        return BookingEntity.findByIdOptional(bookingId, LockModeType.PESSIMISTIC_WRITE)
                .map(BookingEntity.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "Booking with id not found"));
    }

    private void expireBooking(BookingEntity booking) {
        if (booking.status == BookingStatus.PENDING) {
            booking.status = BookingStatus.EXPIRED;
            booking.persist();

            turnSeatsAvailable(booking.id);

            logger.atInfo()
                    .addKeyValue("bookingId", booking.id)
                    .addKeyValue("status", "EXPIRED")
                    .log("[End] expireBookings");
        } else {
            logger.atInfo()
                    .addKeyValue("bookingId", booking.id)
                    .addKeyValue("currentStatus", booking.status)
                    .addKeyValue("status", "SKIPPED")
                    .log("[End] expireBookings");
        }
    }

    private static void turnSeatsAvailable(Long bookingId) {
        var seatsId = getSeatsId(bookingId);

        seatsId.forEach(seatId -> {
            SeatEntity s = SeatEntity.findByIdOptional(seatId)
                    .map(SeatEntity.class::cast)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found", "Seat with id not found"));

            if (s.status == SeatStatus.BOOKED) {
                s.status = SeatStatus.AVAILABLE;
                s.persist();
            }
        });
    }

    private static List<Long> getSeatsId(Long bookingId) {
        return TicketEntity.find("booking.id", bookingId)
                .stream()
                .map(TicketEntity.class::cast)
                .map(t -> t.seat.id)
                .toList();
    }
}
