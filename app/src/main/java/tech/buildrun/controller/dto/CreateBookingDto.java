package tech.buildrun.controller.dto;

import java.util.Set;

public record CreateBookingDto(Long userId,
                               Long eventId,
                               Set<ReserveSeatDto> seats) {
}
