package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.buildrun.controller.dto.CreateAdminRequest;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.AdminException;

import javax.management.relation.Role;

@ApplicationScoped
public class AdminService {

    @Transactional
    public void setupAdminUser(CreateAdminRequest request) {

        var qtdUsers = UserEntity.count();

        if (qtdUsers > 0) {
            throw new AdminException();
        }

        var adminUser = new UserEntity();
        adminUser.username = request.username();
        adminUser.password = BcryptUtil.bcryptHash(request.password());
        adminUser.email = request.email();
        adminUser.role = RoleEntity.find("name = ?1", "admin").firstResult();
        adminUser.persist();
    }
}
