package tech.buildrun.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import tech.buildrun.exception.LoginException;

public record LoginRequestDto(@NotBlank String grantType,

                              @NotBlank
                              @JsonProperty("identifier")
                              @JsonAlias({"username", "clientId"})
                              String identifier,

                              @NotBlank
                              @JsonProperty("secret")
                              @JsonAlias({"password", "clientSecret"})
                              String secret) {
}
