package tech.buildrun.controller.dto;

import tech.buildrun.entity.EventEntity;

public record EventDto(Long id, String name, String description) {

    public static EventDto fromEntity(EventEntity entity) {
        return new EventDto(entity.id, entity.name, entity.description);
    }
}
