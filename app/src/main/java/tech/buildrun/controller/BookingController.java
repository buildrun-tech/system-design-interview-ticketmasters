package tech.buildrun.controller;


import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import tech.buildrun.controller.dto.BookingResponseDto;
import tech.buildrun.controller.dto.ConfirmBookingDto;
import tech.buildrun.controller.dto.CreateBookingDto;
import tech.buildrun.service.BookingService;
import tech.buildrun.service.ConfirmBookingService;

@Path( "/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ConfirmBookingService confirmBookingService;

    public BookingController(BookingService bookingService,
                             ConfirmBookingService confirmBookingService) {
        this.bookingService = bookingService;
        this.confirmBookingService = confirmBookingService;
    }

    @POST
    @RolesAllowed("User")
    public Response createBooking(@Context SecurityContext ctx,
                                  @Valid CreateBookingDto dto) {

        JsonWebToken jwt = (JsonWebToken) ctx.getUserPrincipal();

        var bookingId = bookingService.createBooking(Long.parseLong(jwt.getSubject()), dto);

        return Response.ok(new BookingResponseDto(bookingId)).build();
    }

    @POST
    @Path("/confirm")
    public Response confirmBooking(@Valid ConfirmBookingDto dto) {

        confirmBookingService.confirmBooking(dto.bookingId());

        return Response.noContent().build();
    }
}
