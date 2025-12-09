package tech.buildrun.exception;

public class UpdateBookingException extends TicketMasterException{

    private final String title;
    private final String detail;

    public UpdateBookingException(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        var status = 422;
        return new ProblemDetails(
                new ExceptionResponse(
                        "about:blank",
                        title,
                        detail,
                        status,
                        null
                ),
                status
        );
    }
}
