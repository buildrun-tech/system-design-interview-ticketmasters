package tech.buildrun.controller;


import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.BookingResponseDto;
import tech.buildrun.controller.dto.CreateBookingDto;
import tech.buildrun.service.BookingService;

@Path( "/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @POST
    public Response createBooking(@Valid CreateBookingDto dto) {

        var bookingId = bookingService.createBooking(dto);

        return Response.ok(new BookingResponseDto(bookingId)).build();
    }
}
