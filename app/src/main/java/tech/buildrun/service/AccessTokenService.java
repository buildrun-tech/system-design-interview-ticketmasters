package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.exception.LoginException;
import tech.buildrun.service.strategy.ClientCredentialsTokenStrategy;
import tech.buildrun.service.strategy.PasswordGrantTokenStrategy;
import tech.buildrun.service.strategy.TokenStrategy;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class AccessTokenService {

    private static final Logger logger = getLogger(AccessTokenService.class);

    public static final long EXPIRES_IN = 300;

    private final Map<String, TokenStrategy> tokenStrategy;

    public AccessTokenService(PasswordGrantTokenStrategy passwordGrantTokenStrategy,
                              ClientCredentialsTokenStrategy clientCredentialsTokenStrategy) {

        this.tokenStrategy = Map.of(
                "password", passwordGrantTokenStrategy,
                "client_credentials", clientCredentialsTokenStrategy
        );
    }

    public AccessTokenResponseDto getAccessToken(String grantType,
                                                 String identifier,
                                                 String secret) {

        logger.atInfo()
                .addKeyValue("grantType", grantType)
                .log("[Start] getAccessToken");

        var strategy = tokenStrategy.getOrDefault(grantType, null);

        if (strategy == null) {
            logger.atWarn()
                    .addKeyValue("grantType", grantType)
                    .log("[End] getAccessToken - reason: INVALID_GRANT_TYPE");
            throw new LoginException("Invalid grant type", "Possible values are 'password' or 'client_credentials'");
        }

        var result = strategy.generateToken(identifier, secret);

        logger.atInfo()
                .addKeyValue("grantType", grantType)
                .log("[End] getAccessToken");

        return result;
    }
}
