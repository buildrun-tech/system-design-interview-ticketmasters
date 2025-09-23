package tech.buildrun.controller.dto;

import tech.buildrun.entity.EventEntity;

public record CreateEventDto(String name, String description) {

    public EventEntity toEntity() {
        return new EventEntity(name, description);
    }
}
