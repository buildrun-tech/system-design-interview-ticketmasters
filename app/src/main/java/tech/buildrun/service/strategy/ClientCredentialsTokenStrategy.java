package tech.buildrun.service.strategy;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.entity.AppEntity;
import tech.buildrun.exception.LoginException;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static tech.buildrun.service.AccessTokenService.EXPIRES_IN;

@ApplicationScoped
public class ClientCredentialsTokenStrategy implements TokenStrategy {

    private static final Logger logger = getLogger(ClientCredentialsTokenStrategy.class);

    @Override
    public AccessTokenResponseDto generateToken(String identifier, String secret) {

        logger.atInfo()
                .addKeyValue("grantType", "client_credentials")
                .addKeyValue("identifier", identifier)
                .log("[Start] generateToken");

        var app = AppEntity.find("clientId = ?1", UUID.fromString(identifier))
                .firstResultOptional()
                .map(AppEntity.class::cast)
                .orElseThrow(() -> {
                    logger.atWarn()
                            .addKeyValue("grantType", "client_credentials")
                            .addKeyValue("identifier", identifier)
                            .addKeyValue("reason", "INVALID_CREDENTIALS")
                            .log("[End] generateToken");
                    return new LoginException("Invalid credentials", "Invalid client ID or client secret");
                });

        if (!BcryptUtil.matches(secret, app.clientSecret)) {
            logger.atWarn()
                    .addKeyValue("grantType", "client_credentials")
                    .addKeyValue("identifier", identifier)
                    .addKeyValue("reason", "INVALID_CREDENTIALS")
                    .log("[End] generateToken");
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

        logger.atInfo()
                .addKeyValue("grantType", "client_credentials")
                .addKeyValue("identifier", identifier)
                .log("[End] generateToken");

        return new AccessTokenResponseDto(accessToken, EXPIRES_IN);
    }
}
