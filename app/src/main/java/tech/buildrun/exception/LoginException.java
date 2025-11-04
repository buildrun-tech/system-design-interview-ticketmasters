package tech.buildrun.exception;

public class LoginException extends TicketMasterException{

    private final String title;
    private final String detail;

    public LoginException(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        var status = 401;
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
