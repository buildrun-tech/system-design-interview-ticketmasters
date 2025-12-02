package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.buildrun.controller.dto.CreateAppDto;
import tech.buildrun.controller.dto.CreateAppResponse;
import tech.buildrun.entity.AppEntity;
import tech.buildrun.entity.ScopeEntity;
import tech.buildrun.exception.CreateEntityException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AppService {

    @Transactional
    public CreateAppResponse createApp(CreateAppDto dto) {

        validateAppCreation(dto);

        var entity = new AppEntity();
        var clientSecret = UUID.randomUUID().toString();

        entity.name = dto.name();
        entity.clientId = UUID.randomUUID();
        entity.clientSecret = BcryptUtil.bcryptHash(clientSecret);
        entity.scopes = fetchScopesByName(dto.scopes());

        entity.persist();

        return new CreateAppResponse(entity.id, entity.clientId.toString(), clientSecret);
    }

    private void validateAppCreation(CreateAppDto dto) {
        AppEntity.find("name = ?1", dto.name())
                .firstResultOptional()
                .ifPresent((existingApp) -> {
                    throw new CreateEntityException("Create App Exception", "An app with this name already exists");
                });
    }

    private Set<ScopeEntity> fetchScopesByName(Set<String> scopes) {
        Set<ScopeEntity> scopeEntities = new HashSet<>();

        scopes.forEach((scopeName) -> {

            ScopeEntity.find("name = ?1", scopeName)
                    .firstResultOptional()
                    .map(ScopeEntity.class::cast)
                    .ifPresentOrElse(scopeEntities::add, () -> {
                        throw new CreateEntityException("Create App Excepion", "There is invalid scopes provided");
                    });
        });

        return scopeEntities;
    }
}
