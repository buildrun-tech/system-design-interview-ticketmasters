package tech.buildrun.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.buildrun.controller.dto.CreateUserDto;
import tech.buildrun.entity.RoleEntity;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.exception.CreateEntityException;

@ApplicationScoped
public class UserService {

    @Transactional
    public UserEntity createUser(CreateUserDto dto) {

        var qtdUsers = UserEntity.count("username = ?1 OR email = ?2", dto.username(), dto.email());

        if (qtdUsers > 0) {
            throw new CreateEntityException("Create User Exception", "User already exists with this username or email");
        }

        var entity = new UserEntity();
        entity.username = dto.username();
        entity.email = dto.email();
        entity.password = BcryptUtil.bcryptHash(dto.password());
        entity.role = RoleEntity.find("name = ?1", "user").firstResult();

        entity.persist();

        return entity;
    }
}
