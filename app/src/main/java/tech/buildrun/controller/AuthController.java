package tech.buildrun.controller;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.LoginRequestDto;
import tech.buildrun.service.AccessTokenService;

@Path("/auth")
public class AuthController {

    private final AccessTokenService accessTokenService;
    private final Validator validator;

    public AuthController(AccessTokenService accessTokenService,
                          Validator validator) {
        this.accessTokenService = accessTokenService;
        this.validator = validator;
    }

    @POST
    @Path("/token")
    @RunOnVirtualThread
    public Response getToken(@Valid LoginRequestDto dto) {

        var body = accessTokenService.getAccessToken(dto.grantType(), dto.identifier(), dto.secret());

        return Response.ok(body).build();
    }
}
