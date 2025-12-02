package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.exception.LoginException;
import tech.buildrun.service.strategy.ClientCredentialsTokenStrategy;
import tech.buildrun.service.strategy.PasswordGrantTokenStrategy;
import tech.buildrun.service.strategy.TokenStrategy;

import java.util.*;

@ApplicationScoped
public class AccessTokenService {

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

        var strategy = tokenStrategy.getOrDefault(grantType, null);

        if (strategy == null) {
            throw new LoginException("Invalid grant type", "Possible values are 'password' or 'client_credentials'");
        }

        return strategy.generateToken(identifier, secret);
    }
}
