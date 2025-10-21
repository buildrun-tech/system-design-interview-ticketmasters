package tech.buildrun.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.buildrun.entity.EventEntity;

public record CreateEventDto(@NotBlank String name,
                             @NotBlank String description,
                             @NotNull @Valid EventSettingDto settings) {

    public EventEntity toEntity() {
        return new EventEntity(name, description);
    }
}
