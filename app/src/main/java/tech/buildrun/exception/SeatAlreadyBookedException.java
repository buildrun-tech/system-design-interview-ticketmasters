package tech.buildrun.exception;

public class SeatAlreadyBookedException extends TicketMasterException {

    private String seatId;

    public SeatAlreadyBookedException(String seatId) {
        this.seatId = seatId;
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        var status = 422;
        return new ProblemDetails(
                new ExceptionResponse(
                        "about:blank",
                        "Seat already booked",
                        "This seat " + seatId + " is already booked",
                        status,
                        null
                ),
                status
        );
    }
}
