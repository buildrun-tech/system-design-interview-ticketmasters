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
import tech.buildrun.controller.dto.RejectBookingDto;
import tech.buildrun.entity.BookingStatus;
import tech.buildrun.service.BookingService;
import tech.buildrun.service.UpdateBookingStatusService;

@Path( "/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UpdateBookingStatusService updateBookingStatusService;

    public BookingController(BookingService bookingService,
                             UpdateBookingStatusService updateBookingStatusService) {
        this.bookingService = bookingService;
        this.updateBookingStatusService = updateBookingStatusService;
    }

    @POST
    @RolesAllowed({"admin", "bookings:reserve"})
    public Response createBooking(@Context SecurityContext ctx,
                                  @Valid CreateBookingDto dto) {

        JsonWebToken jwt = (JsonWebToken) ctx.getUserPrincipal();

        var bookingId = bookingService.createBooking(Long.parseLong(jwt.getSubject()), dto);

        return Response.ok(new BookingResponseDto(bookingId)).build();
    }

    @POST
    @RolesAllowed({"admin", "bookings:confirm"})
    @Path("/confirm")
    public Response confirmBooking(@Valid ConfirmBookingDto dto) {

        updateBookingStatusService.updateBookingStatus(dto.bookingId(), BookingStatus.CONFIRMED);

        return Response.noContent().build();
    }

    @POST
    @RolesAllowed({"admin", "bookings:reject"})
    @Path("/reject")
    public Response rejectBooking(@Valid RejectBookingDto dto) {

        updateBookingStatusService.updateBookingStatus(dto.bookingId(), BookingStatus.REJECTED);

        return Response.noContent().build();
    }
}
