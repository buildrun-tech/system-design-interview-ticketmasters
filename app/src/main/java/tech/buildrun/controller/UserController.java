package tech.buildrun.controller;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import tech.buildrun.entity.UserEntity;

import java.util.List;

@Path("/users")
public class UserController {

    @POST
    @Transactional
    public void createUser() {
        var user = new UserEntity();
        user.username = "joao";
        user.email = "joao@teste.com";
        user.password = BcryptUtil.bcryptHash("123");

        user.persist();
    }

    @GET
    public List<UserEntity> getUsers() {
        return UserEntity.listAll();
    }
}
