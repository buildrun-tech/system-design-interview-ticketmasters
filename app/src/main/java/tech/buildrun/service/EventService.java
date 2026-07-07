package tech.buildrun.service;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.*;
import tech.buildrun.entity.EventEntity;
import tech.buildrun.entity.SeatEntity;
import tech.buildrun.entity.SeatStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class EventService {

    private static final Logger logger = getLogger(EventService.class);

    public ApiListDto<EventDto> findAll(int page, int pageSize) {
        var query = EventEntity.findAll()
                .page(Page.of(page, pageSize));

        var totalPages = query.pageCount();
        var totalElements = query.count();
        var events = query.stream()
                .map(EventEntity.class::cast)
                .map(EventDto::fromEntity)
                .toList();

        return new ApiListDto<>(
                events,
                new PaginationDto(page, pageSize, totalPages, totalElements)
        );
    }

    @Transactional
    public EventDto createEvent(CreateEventDto dto) {

        logger.atInfo()
                .addKeyValue("eventName", dto.name())
                .addKeyValue("numberOfSeats", dto.settings().numberOfSeats())
                .log("[Start] createEvent");

        var eventEntity = dto.toEntity();

        eventEntity.persist();

        SeatEntity entitySeat;
        for (int c = 0; c < dto.settings().numberOfSeats(); c++) {

            var seatName = "S" + c;
            entitySeat = new SeatEntity(eventEntity, seatName, SeatStatus.AVAILABLE);
            entitySeat.persist();
        }

        var result = EventDto.fromEntity(eventEntity);

        logger.atInfo()
                .addKeyValue("eventId", result.id())
                .addKeyValue("eventName", result.name())
                .log("[End] createEvent");

        return result;
    }

    public Optional<EventDto> findById(Long id) {

        logger.atInfo()
                .addKeyValue("eventId", id)
                .log("[Start] findById");

        var result = EventEntity.findByIdOptional(id)
                .map(EventEntity.class::cast)
                .map(EventDto::fromEntity);

        if (result.isEmpty()) {
            logger.atWarn()
                    .addKeyValue("eventId", id)
                    .addKeyValue("reason", "NOT_FOUND")
                    .log("[End] findById");
        } else {
            logger.atInfo()
                    .addKeyValue("eventId", id)
                    .log("[End] findById");
        }

        return result;
    }

    public ApiListDto<SeatDto> findAllSeats(Long eventId,
                                            Integer page,
                                            Integer pageSize) {

        // TODO - tratar exception via ExceptionHandler
        var event = EventEntity.findByIdOptional(eventId).orElseThrow();

        var query = SeatEntity.find("event", event)
                .page(Page.of(page, pageSize));

        var totalPages = query.pageCount();
        var totalElements = query.count();
        var events = query.stream()
                .map(SeatEntity.class::cast)
                .map(SeatDto::fromEntity)
                .toList();

        return new ApiListDto<>(
                events,
                new PaginationDto(page, pageSize, totalPages, totalElements)
        );
    }
}
