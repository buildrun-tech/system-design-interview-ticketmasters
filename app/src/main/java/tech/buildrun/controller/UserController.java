package tech.buildrun.controller;

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
        user.username = "bruno";
        user.email = "bruno@teste.com";
        user.password = "123";

        user.persist();
    }

    @GET
    public List<UserEntity> getUsers() {
        return UserEntity.listAll();
    }
}
