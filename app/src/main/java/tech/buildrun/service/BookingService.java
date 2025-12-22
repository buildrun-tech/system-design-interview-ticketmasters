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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.slf4j.LoggerFactory.*;

@ApplicationScoped
public class BookingService {

    private static final Logger logger = getLogger(BookingService.class);

    private static final ConcurrentHashMap<String, ReentrantLock> seatLocks = new ConcurrentHashMap<>();

    private final BookingExpirationService bookingExpirationService;

    public BookingService(BookingExpirationService bookingExpirationService) {
        this.bookingExpirationService = bookingExpirationService;
    }


    // TODO - validar cenarios de concorrencia
    @Transactional
    public Long createBooking(Long userId, CreateBookingDto dto) {

        validateInputs(userId, dto);

        Long seatId = dto.seats().iterator().next().seatId();

        String lockKey = dto.eventId() + ":" + seatId;

        ReentrantLock lock = seatLocks.computeIfAbsent(lockKey, k -> new ReentrantLock(true));

        if (!lock.tryLock())
            throw new SeatAlreadyBookedException("seatId=" + seatId);

        try {
            Set<SeatEntity> seatsAvailable = getSeatsAvailable(dto);

            var bookingEntity = buildBookingEntity(userId, dto);
            bookingEntity.persist();

            createTickets(seatsAvailable, bookingEntity);

            updateSeats(seatsAvailable);

            bookingEntity.getEntityManager().flush();

            bookingExpirationService.scheduleExpirationCheck(bookingEntity.id);

            return bookingEntity.id;
        } finally {
            lock.unlock();
        }
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
