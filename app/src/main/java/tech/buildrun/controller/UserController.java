package tech.buildrun.controller;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.CreateUserDto;
import tech.buildrun.entity.UserEntity;
import tech.buildrun.service.UserService;

import java.net.URI;
import java.util.List;

@Path("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @POST
    public Response createUser(@Valid CreateUserDto dto) {

        var user = userService.createUser(dto);

        var location = URI.create("/users/" + user.id);

        return Response.created(location).build();
    }

    @GET
    public List<UserEntity> getUsers() {
        return UserEntity.listAll();
    }
}
