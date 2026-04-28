package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.CreateUserDto;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.CreateEntityException;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class UserService {

    private static final Logger logger = getLogger(UserService.class);

    @Transactional
    public UserEntity createUser(CreateUserDto dto) {

        logger.atInfo()
                .addKeyValue("username", dto.username())
                .addKeyValue("email", dto.email())
                .log("[Start] createUser");

        var qtdUsers = UserEntity.count("username = ?1 OR email = ?2", dto.username(), dto.email());

        if (qtdUsers > 0) {
            logger.atWarn()
                    .addKeyValue("username", dto.username())
                    .addKeyValue("email", dto.email())
                    .addKeyValue("reason", "DUPLICATE_USER")
                    .log("[End] createUser");
            throw new CreateEntityException("Create User Exception", "User already exists with this username or email");
        }

        var entity = new UserEntity();
        entity.username = dto.username();
        entity.email = dto.email();
        entity.password = BcryptUtil.bcryptHash(dto.password());
        entity.role = RoleEntity.find("name = ?1", "user").firstResult();

        UserEntity.persist(entity);

        logger.atInfo()
                .addKeyValue("username", entity.username)
                .addKeyValue("userId", entity.id)
                .log("[End] createUser");

        return entity;
    }
}
