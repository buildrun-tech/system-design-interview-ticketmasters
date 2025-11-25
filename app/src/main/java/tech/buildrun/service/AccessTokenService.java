package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.LoginException;
import tech.buildrun.exception.ResourceNotFoundException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class AccessTokenService {

    private static final long EXPIRES_IN = 300;

    public AccessTokenResponseDto getAccessToken(LoginRequestDto dto) {

        // TODO - reavaliar if-esle-if-else aqui, talvez usar Strategy Pattern
        if (dto.grantType().equals("password")) {
            return getAccessTokenPassword(dto.username(), dto.password());

        } else if (dto.grantType().equals("client_credentials")) {
            return getAccessTokenClientCredentials(dto.clientId(), dto.clientSecret());

        } else {
            throw new LoginException("Invalid grant type", "Possible values are 'password' or 'client_credentials'");
        }

    }

    private AccessTokenResponseDto getAccessTokenClientCredentials(String clientId, String clientSecret) {
        // TODO - Implementar grant type client credentials
        throw new LoginException("Login Exception", "Client credentials grant type not implemented");
    }

    private AccessTokenResponseDto getAccessTokenPassword(String username, String password) {
        var user = UserEntity.find("username = ?1", username)
                .firstResultOptional()
                .map(UserEntity.class::cast)
                .orElseThrow(() -> new LoginException("Invalid credentials", "Invalid username or password"));

        if (!BcryptUtil.matches(password, user.password)) {
            throw new LoginException("Invalid credentials", "Invalid username or password");
        }

        var accessToken = Jwt.issuer("ticketmaster")
                .upn(username)
                .subject(user.id.toString())
                .groups(new HashSet<>(List.of("User")))
                .claim("email", user.email)
                .expiresIn(EXPIRES_IN)
                .sign();

        return new AccessTokenResponseDto(accessToken, EXPIRES_IN);
    }
}
