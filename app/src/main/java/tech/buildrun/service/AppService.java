package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.CreateAppDto;
import tech.buildrun.controller.dto.CreateAppResponse;
import tech.buildrun.entity.AppEntity;
import tech.buildrun.entity.ScopeEntity;
import tech.buildrun.exception.CreateEntityException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class AppService {

    private static final Logger logger = getLogger(AppService.class);

    @Transactional
    public CreateAppResponse createApp(CreateAppDto dto) {

        logger.atInfo()
                .addKeyValue("appName", dto.name())
                .addKeyValue("scopes", dto.scopes())
                .log("[Start] createApp");

        validateAppCreation(dto);

        var entity = new AppEntity();
        var clientSecret = UUID.randomUUID().toString();

        entity.name = dto.name();
        entity.clientId = UUID.randomUUID();
        entity.clientSecret = BcryptUtil.bcryptHash(clientSecret);
        entity.scopes = fetchScopesByName(dto.scopes());

        entity.persist();

        logger.atInfo()
                .addKeyValue("appName", entity.name)
                .addKeyValue("clientId", entity.clientId)
                .log("[End] createApp");

        return new CreateAppResponse(entity.id, entity.clientId.toString(), clientSecret);
    }

    private void validateAppCreation(CreateAppDto dto) {
        AppEntity.find("name = ?1", dto.name())
                .firstResultOptional()
                .ifPresent((existingApp) -> {
                    logger.atWarn()
                            .addKeyValue("appName", dto.name())
                            .addKeyValue("reason", "DUPLICATE_APP_NAME")
                            .log("[End] createApp");
                    throw new CreateEntityException("Create App Exception", "An app with this name already exists");
                });
    }

    private Set<ScopeEntity> fetchScopesByName(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new CreateEntityException("Create App Exception", "No scopes provided");
        }

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
