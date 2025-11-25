package tech.buildrun.controller;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.CreateAppDto;
import tech.buildrun.service.AppService;

import java.net.URI;

@Path("/apps")
public class AppController {

    private final AppService appService;

    public AppController(AppService appService) {
        this.appService = appService;
    }

    @POST
    public Response createApp(CreateAppDto dto) {

        var resp = appService.createApp(dto);

        return Response.created(null).build();
    }
}
