package tech.buildrun.service.strategy;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.entity.AppEntity;
import tech.buildrun.exception.LoginException;

import java.util.UUID;
import java.util.stream.Collectors;

import static tech.buildrun.service.AccessTokenService.EXPIRES_IN;

@ApplicationScoped
public class ClientCredentialsTokenStrategy implements TokenStrategy {

    @Override
    public AccessTokenResponseDto generateToken(String identifier, String secret) {

        var app = AppEntity.find("clientId = ?1", UUID.fromString(identifier))
                .firstResultOptional()
                .map(AppEntity.class::cast)
                .orElseThrow(() -> new LoginException("Invalid credentials", "Invalid client ID or client secret"));

        if (!BcryptUtil.matches(secret, app.clientSecret)) {
            throw new LoginException("Invalid credentials", "Invalid client ID or client secret");
        }

        var groups = app.scopes.stream()
                .map(scope -> scope.name)
                .collect(Collectors.toSet());

        var accessToken = Jwt.issuer("ticketmaster")
                .upn(app.clientId.toString())
                .subject(app.id.toString())
                .groups(groups)
                .claim("app_name", app.name)
                .expiresIn(EXPIRES_IN)
                .sign();

        return new AccessTokenResponseDto(accessToken, EXPIRES_IN);
    }
}
