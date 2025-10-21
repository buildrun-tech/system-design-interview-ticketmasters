package tech.buildrun.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EventSettingDto(@NotNull @Min(1) Integer numberOfSeats) {
}
