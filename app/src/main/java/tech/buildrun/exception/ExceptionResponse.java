package tech.buildrun.exception;

import tech.buildrun.exception.dto.InvalidParamResponse;

import java.util.List;

public record ExceptionResponse(String type,
                                String title,
                                String detail,
                                Integer status,
                                List<InvalidParamResponse> invalidParams) {
}
