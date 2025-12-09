package tech.buildrun.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RejectBookingDto(@NotNull @Min(1) Long bookingId) {
}
