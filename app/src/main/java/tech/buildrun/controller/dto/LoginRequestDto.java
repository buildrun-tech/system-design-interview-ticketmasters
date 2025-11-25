package tech.buildrun.controller.dto;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import tech.buildrun.exception.LoginException;

public record LoginRequestDto(@NotBlank String grantType,
                              @NotBlank(groups = PasswordGrant.class) String username,
                              @NotBlank(groups = PasswordGrant.class) String password,
                              @NotBlank(groups = ClientCredentialsGrant.class) String clientId,
                              @NotBlank(groups = ClientCredentialsGrant.class) String clientSecret) {

    public void validate(Validator validator) {

        // TODO - usar strategy pattern aqui

        if (grantType.equals("password")) {
            var violations = validator.validate(this, PasswordGrant.class);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

        } else if (grantType.equals("client_credentials")) {
            var violations = validator.validate(this, ClientCredentialsGrant.class);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

        } else {
            throw new LoginException("Invalid grant type", "Possible values are 'password' or 'client_credentials'");
        }
    }
}
