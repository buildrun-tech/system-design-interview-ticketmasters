package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.buildrun.entity.*;
import tech.buildrun.exception.ResourceNotFoundException;

import java.util.List;

@ApplicationScoped
public class ExpireBookingService {

    @Transactional
    public void expireBookings(Long bookingId) {

        var booking = findBooking(bookingId);

        expireBooking(booking);

        turnSeatsAvailable(bookingId);
    }

    private static BookingEntity findBooking(Long bookingId) {
        return BookingEntity.findByIdOptional(bookingId)
                .map(BookingEntity.class::cast)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "Booking with id not found"));
    }

    private static void expireBooking(BookingEntity booking) {
        if (booking.status == BookingStatus.PENDING) {
            booking.status = BookingStatus.EXPIRED;
            booking.persist();
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
