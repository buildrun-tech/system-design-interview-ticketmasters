package tech.buildrun.service.strategy;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.LoginException;

import java.util.HashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static tech.buildrun.service.AccessTokenService.EXPIRES_IN;

@ApplicationScoped
public class PasswordGrantTokenStrategy implements TokenStrategy {

    private static final Logger logger = getLogger(PasswordGrantTokenStrategy.class);

    @Override
    public AccessTokenResponseDto generateToken(String identifier, String secret) {

        logger.atInfo()
                .addKeyValue("grantType", "password")
                .addKeyValue("identifier", identifier)
                .log("[Start] generateToken");

        var user = UserEntity.find("username = ?1", identifier)
                .firstResultOptional()
                .map(UserEntity.class::cast)
                .orElseThrow(() -> {
                    logger.atWarn()
                            .addKeyValue("grantType", "password")
                            .addKeyValue("identifier", identifier)
                            .addKeyValue("reason", "INVALID_CREDENTIALS")
                            .log("[End] generateToken");
                    return new LoginException("Invalid credentials", "Invalid username or password");
                });

        if (!BcryptUtil.matches(secret, user.password)) {
            logger.atWarn()
                    .addKeyValue("grantType", "password")
                    .addKeyValue("identifier", identifier)
                    .addKeyValue("reason", "INVALID_CREDENTIALS")
                    .log("[End] generateToken");
            throw new LoginException("Invalid credentials", "Invalid username or password");
        }

        var groups = extractGroupsFromRoleScopes(user.role);

        var accessToken = Jwt.issuer("ticketmaster")
                .upn(identifier)
                .subject(user.id.toString())
                .groups(groups)
                .claim("email", user.email)
                .expiresIn(EXPIRES_IN)
                .sign();

        logger.atInfo()
                .addKeyValue("grantType", "password")
                .addKeyValue("identifier", identifier)
                .log("[End] generateToken");

        return new AccessTokenResponseDto(accessToken, EXPIRES_IN);
    }

    private Set<String> extractGroupsFromRoleScopes(RoleEntity role) {
        Set<String> groups = new HashSet<>();

        groups.add(role.name);

        role.scopes.forEach(group -> groups.add(group.name));

        return groups;
    }
}
