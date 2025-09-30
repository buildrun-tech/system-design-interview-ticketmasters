package tech.buildrun.controller.dto;

import tech.buildrun.entity.EventEntity;

public record CreateEventDto(String name, String description, EventSettingDto settings) {

    public EventEntity toEntity() {
        return new EventEntity(name, description);
    }
}
