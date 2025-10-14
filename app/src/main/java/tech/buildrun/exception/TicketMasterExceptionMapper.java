package tech.buildrun.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TicketMasterExceptionMapper implements ExceptionMapper<TicketMasterException> {

    @Override
    public Response toResponse(TicketMasterException e) {
        return Response
                .status(e.toProblemDetails().status())
                .entity(e.toProblemDetails().response())
                .build();
    }
}
