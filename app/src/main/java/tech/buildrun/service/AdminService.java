package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import tech.buildrun.controller.dto.CreateAdminRequest;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.AdminException;

import static org.slf4j.LoggerFactory.getLogger;

@ApplicationScoped
public class AdminService {

    private static final Logger logger = getLogger(AdminService.class);

    @Transactional
    public void setupAdminUser(CreateAdminRequest request) {

        logger.atInfo()
                .addKeyValue("username", request.username())
                .log("[Start] setupAdminUser");

        var qtdUsers = UserEntity.count();

        if (qtdUsers > 0) {
            logger.atWarn()
                    .addKeyValue("username", request.username())
                    .addKeyValue("reason", "ADMIN_ALREADY_EXISTS")
                    .log("[End] setupAdminUser");
            throw new AdminException();
        }

        var adminUser = new UserEntity();
        adminUser.username = request.username();
        adminUser.password = BcryptUtil.bcryptHash(request.password());
        adminUser.email = request.email();
        adminUser.role = RoleEntity.find("name = ?1", "admin").firstResult();
        adminUser.persist();

        logger.atInfo()
                .addKeyValue("username", adminUser.username)
                .addKeyValue("userId", adminUser.id)
                .log("[End] setupAdminUser");
    }
}
