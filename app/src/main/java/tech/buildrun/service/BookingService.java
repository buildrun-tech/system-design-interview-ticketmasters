package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.CreateBookingDto;
import tech.buildrun.controller.dto.ReserveSeatDto;
import tech.buildrun.entity.*;
import tech.buildrun.exception.ResourceNotFoundException;
import tech.buildrun.exception.SeatAlreadyBookedException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.*;

@ApplicationScoped
public class BookingService {

    private static final Logger logger = getLogger(BookingService.class);

    private final BookingExpirationService bookingExpirationService;

    public BookingService(BookingExpirationService bookingExpirationService) {
        this.bookingExpirationService = bookingExpirationService;
    }


    // TODO - validar cenarios de concorrencia
    @Transactional
    public Long createBooking(CreateBookingDto dto) {

        validateInputs(dto);

        Set<SeatEntity> seatsAvailable = getSeatsAvailable(dto);

        var bookingEntity = buildBookingEntity(dto);
        bookingEntity.persist();

        createTickets(seatsAvailable, bookingEntity);

        updateSeats(seatsAvailable);

        bookingExpirationService.scheduleExpirationCheck(bookingEntity.id);

        return bookingEntity.id;
    }

    private static Set<SeatEntity> getSeatsAvailable(CreateBookingDto dto) {
        Set<SeatEntity> seatsAvailable = new HashSet<>();
        dto.seats()
                .forEach(seat -> {

                        SeatEntity s = SeatEntity.findByIdOptional(seat.seatId())
                                .map(SeatEntity.class::cast)
                                .orElseThrow(() -> new ResourceNotFoundException("Seat not found", "Seat with id not found"));

                        if (s.status == SeatStatus.BOOKED) {
                            throw new SeatAlreadyBookedException(s.name);
                        }

                        seatsAvailable.add(s);
                });
        return seatsAvailable;
    }

    private static void validateInputs(CreateBookingDto dto) {
        UserEntity.findByIdOptional(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "User with id not found"));

        EventEntity.findByIdOptional(dto.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", "Event with id not found"));
    }

    private static BookingEntity buildBookingEntity(CreateBookingDto dto) {
        BookingEntity booking = new BookingEntity();

        booking.bookedAt = Instant.now();
        booking.status = BookingStatus.PENDING;
        booking.user = UserEntity.findById(dto.userId());

        return booking;
    }

    private static void createTickets(Set<SeatEntity> seats, BookingEntity bookingEntity) {
        TicketEntity ticketEntity = new TicketEntity();
        seats.forEach(seat -> {
            ticketEntity.seat = seat;
            ticketEntity.externalId = UUID.randomUUID();
            ticketEntity.booking = bookingEntity;
            ticketEntity.persist();
        });
    }

    private static void updateSeats(Set<SeatEntity> seats) {
        seats.stream()
                .peek(seat -> seat.status = SeatStatus.BOOKED)
                .forEach(seat -> seat.persist());
    }


}
