package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
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

    @Transactional
    public Long createBooking(Long userId, CreateBookingDto dto) {

        var seatIds = dto.seats().stream().map(ReserveSeatDto::seatId).toList();

        logger.atInfo()
                .addKeyValue("userId", userId)
                .addKeyValue("eventId", dto.eventId())
                .addKeyValue("seats", seatIds)
                .log("Start booking");

        validateInputs(userId, dto);

        Set<SeatEntity> seatsAvailable = getSeatsAvailable(dto);

        var bookingEntity = buildBookingEntity(userId, dto);
        bookingEntity.persist();

        createTickets(seatsAvailable, bookingEntity);

        updateSeats(seatsAvailable);

        bookingExpirationService.scheduleExpirationCheck(bookingEntity.id);

        logger.atInfo()
                .addKeyValue("userId", userId)
                .addKeyValue("eventId", dto.eventId())
                .addKeyValue("seats", seatIds)
                .addKeyValue("bookingId", bookingEntity.id)
                .log("Booking finished");

        return bookingEntity.id;
    }

    private static Set<SeatEntity> getSeatsAvailable(CreateBookingDto dto) {
        Set<SeatEntity> seatsAvailable = new HashSet<>();
        dto.seats()
                .forEach(seat -> {

                        SeatEntity s = SeatEntity.findByIdOptional(seat.seatId(), LockModeType.PESSIMISTIC_WRITE)
                                .map(SeatEntity.class::cast)
                                .orElseThrow(() -> new ResourceNotFoundException("Seat not found", "Seat with id not found"));

                        if (s.status == SeatStatus.BOOKED) {
                            throw new SeatAlreadyBookedException(s.name);
                        }

                        seatsAvailable.add(s);
                });
        return seatsAvailable;
    }

    private static void validateInputs(Long userId, CreateBookingDto dto) {
        UserEntity.findByIdOptional(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "User with id not found"));

        EventEntity.findByIdOptional(dto.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", "Event with id not found"));
    }

    private static BookingEntity buildBookingEntity(Long userId, CreateBookingDto dto) {
        BookingEntity booking = new BookingEntity();

        booking.bookedAt = Instant.now();
        booking.status = BookingStatus.PENDING;
        booking.user = UserEntity.findById(userId);

        return booking;
    }

    private static void createTickets(Set<SeatEntity> seats, BookingEntity bookingEntity) {
        seats.forEach(seat -> {
            TicketEntity ticketEntity = new TicketEntity();
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
