package tech.buildrun.exception;

import jakarta.ws.rs.core.Response;

public class TicketMasterException extends RuntimeException{

    protected ProblemDetails toProblemDetails() {
        return new ProblemDetails(
                new ExceptionResponse("InternalServerError", "TicketMaster Exception", ""),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
        );
    }
}
