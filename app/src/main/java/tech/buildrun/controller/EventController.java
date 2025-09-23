package tech.buildrun.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.CreateEventDto;
import tech.buildrun.controller.dto.EventDto;
import tech.buildrun.service.EventService;

import java.net.URI;
import java.util.List;

@Path( "/events")
public class EventController {

    private EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GET
    public List<EventDto> getEvents() {
        return eventService.findAll();
    }

    @POST
    public Response createEvent(CreateEventDto dto) {

        var body = eventService.createEvent(dto);

        var location = URI.create(String.format("/events/%s", body.id()));

        return Response.created(location).build();
    }

    @GET
    @Path("/{id}")
    public Response getEvent(Long id) {

        var event = eventService.findById(id);

        return event.isPresent()
                ? Response.ok(event.get()).build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

}
