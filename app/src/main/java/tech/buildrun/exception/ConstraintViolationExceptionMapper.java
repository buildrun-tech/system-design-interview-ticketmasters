package tech.buildrun.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import tech.buildrun.exception.dto.InvalidParamResponse;

import static org.slf4j.LoggerFactory.getLogger;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger logger = getLogger(ConstraintViolationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var status = 400;
        var invalidParams = exception.getConstraintViolations()
                .stream()
                .map(cv -> {
                    var fullPath = cv.getPropertyPath().toString();
                    var paramName = fullPath.substring(fullPath.lastIndexOf(".") + 1);

                    return new InvalidParamResponse(paramName, cv.getMessage());
                })
                .toList();

        var violatedFields = invalidParams.stream()
                .map(InvalidParamResponse::name)
                .toList();

        logger.atWarn()
                .addKeyValue("httpStatus", status)
                .addKeyValue("violatedFields", violatedFields)
                .log("Constraint violation");

        return Response
                .status(status)
                .entity(
                        new ExceptionResponse(
                            "about:blank",
                            "Constraint Violation",
                            "There is a constraint violation in the request",
                                status,
                                invalidParams
                        )
                )
                .build();
    }
}
