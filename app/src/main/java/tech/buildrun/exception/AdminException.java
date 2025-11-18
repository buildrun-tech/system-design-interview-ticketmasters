package tech.buildrun.exception;

public class AdminException extends TicketMasterException{


    public AdminException() {
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        var status = 422;
        return new ProblemDetails(
                new ExceptionResponse(
                        "about:blank",
                        "Admin creation exception",
                        "Admin user can only be created when there are no users in the system.",
                        status,
                        null
                ),
                status
        );
    }
}
