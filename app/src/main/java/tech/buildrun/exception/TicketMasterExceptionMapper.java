package tech.buildrun.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Provider
public class TicketMasterExceptionMapper implements ExceptionMapper<TicketMasterException> {

    private static final Logger logger = getLogger(TicketMasterExceptionMapper.class);

    @Override
    public Response toResponse(TicketMasterException e) {
        var problemDetails = e.toProblemDetails();

        logger.atWarn()
                .addKeyValue("exceptionType", e.getClass().getSimpleName())
                .addKeyValue("httpStatus", problemDetails.status())
                .log(e.getMessage());

        return Response
                .status(problemDetails.status())
                .entity(problemDetails.response())
                .build();
    }
}
