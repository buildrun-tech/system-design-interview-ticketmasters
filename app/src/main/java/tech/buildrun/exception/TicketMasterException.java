package tech.buildrun.exception;

import jakarta.ws.rs.core.Response;

public class TicketMasterException extends RuntimeException{

    protected ProblemDetails toProblemDetails() {
        return new ProblemDetails(
                new ExceptionResponse(
                        "about:blank",
                        "TicketMaster Exception",
                        "There is a internal server error",
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        null
                ),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
        );
    }
}
