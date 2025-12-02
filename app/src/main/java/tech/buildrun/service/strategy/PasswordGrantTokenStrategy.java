package tech.buildrun.service.strategy;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.LoginException;

import java.util.HashSet;
import java.util.Set;

import static tech.buildrun.service.AccessTokenService.EXPIRES_IN;

@ApplicationScoped
public class PasswordGrantTokenStrategy implements TokenStrategy {

    @Override
    public AccessTokenResponseDto generateToken(String identifier, String secret) {

        var user = UserEntity.find("username = ?1", identifier)
                .firstResultOptional()
                .map(UserEntity.class::cast)
                .orElseThrow(() -> new LoginException("Invalid credentials", "Invalid username or password"));

        if (!BcryptUtil.matches(secret, user.password)) {
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

        return new AccessTokenResponseDto(accessToken, EXPIRES_IN);
    }

    private Set<String> extractGroupsFromRoleScopes(RoleEntity role) {
        Set<String> groups = new HashSet<>();

        groups.add(role.name);

        role.scopes.forEach(group -> groups.add(group.name));

        return groups;
    }
}
