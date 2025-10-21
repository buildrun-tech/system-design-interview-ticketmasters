package tech.buildrun.controller.dto;

import jakarta.validation.constraints.NotNull;

public record ReserveSeatDto(@NotNull Long seatId) {
}
