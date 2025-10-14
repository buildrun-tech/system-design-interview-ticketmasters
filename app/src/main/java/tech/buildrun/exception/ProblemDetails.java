package tech.buildrun.exception;

import io.netty.handler.codec.http.HttpStatusClass;
import jakarta.ws.rs.core.Response;

public record ProblemDetails(ExceptionResponse response, int status) {
}
