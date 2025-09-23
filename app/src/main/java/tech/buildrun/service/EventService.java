package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.buildrun.controller.dto.CreateEventDto;
import tech.buildrun.controller.dto.EventDto;
import tech.buildrun.entity.EventEntity;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventService {

    public List<EventDto> findAll() {
        return EventEntity.listAll()
                .stream()
                .map(EventEntity.class::cast)
                .map(EventDto::fromEntity)
                .toList();
    }

    @Transactional
    public EventDto createEvent(CreateEventDto dto) {
        var entity = dto.toEntity();

        entity.persist();

        return EventDto.fromEntity(entity);
    }

    public Optional<EventDto> findById(Long id) {

        return EventEntity.findByIdOptional(id)
                .map(EventEntity.class::cast)
                .map(EventDto::fromEntity);
    }
}
