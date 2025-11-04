package tech.buildrun.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateBookingDto(@NotNull Long eventId,
                               @NotNull @Size(min = 1) Set<ReserveSeatDto> seats) {
}
