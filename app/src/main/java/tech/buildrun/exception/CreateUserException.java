package tech.buildrun.exception;

public class CreateUserException extends TicketMasterException{

    private final String detail;

    public CreateUserException(String detail) {
        this.detail = detail;
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        var status = 422;
        return new ProblemDetails(
                new ExceptionResponse(
                        "about:blank",
                        "Create User Exception",
                        detail,
                        status,
                        null
                ),
                status
        );
    }
}
