package tech.buildrun.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.*;
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
    public ApiListDto<EventDto> getEvents(@QueryParam("page") @DefaultValue("0") Integer page,
                                          @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {

        return eventService.findAll(page, pageSize);
    }

    @POST
    @RolesAllowed({"admin", "events:create"})
    public Response createEvent(@Valid CreateEventDto dto) {

        var body = eventService.createEvent(dto);

        var location = URI.create(String.format("/events/%s", body.id()));

        return Response.created(location).build();
    }

    @GET
    @RolesAllowed({"admin", "events:read"})
    @Path("/{id}")
    public Response getEvent(@PathParam("id") Long id) {

        var event = eventService.findById(id);

        return event.isPresent()
                ? Response.ok(event.get()).build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @RolesAllowed({"admin", "seats:list"})
    @Path("/{id}/seats")
    public ApiListDto<SeatDto> getSeats(@PathParam("id") Long id,
                                        @QueryParam("page") @DefaultValue("0") Integer page,
                                        @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {

        return eventService.findAllSeats(id, page, pageSize);
    }

}
