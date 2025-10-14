package tech.buildrun.exception;

public class SeatAlreadyBookedException extends TicketMasterException {

    private String seatId;

    public SeatAlreadyBookedException(String seatId) {
        this.seatId = seatId;
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        return new ProblemDetails(
                new ExceptionResponse("SeatAlreadyBookedException", "Seat already booked", "This seat " + seatId + " is already booked"),
                422
        );
    }
}
