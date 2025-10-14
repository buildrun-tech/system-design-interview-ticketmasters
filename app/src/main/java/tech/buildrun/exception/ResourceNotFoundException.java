package tech.buildrun.exception;

import jakarta.ws.rs.core.Response;

public class ResourceNotFoundException extends TicketMasterException{

    private final String title;
    private final String detail;

    public ResourceNotFoundException(String title, String detail) {
        this.title = title;
        this.detail = detail;
    }

    @Override
    protected ProblemDetails toProblemDetails() {
        return new ProblemDetails(
                new ExceptionResponse("ResourceNotFoundException", title, detail),
                422
        );
    }
}
