package tech.buildrun.controller;

import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.buildrun.controller.dto.CreateAdminRequest;
import tech.buildrun.service.AdminService;

@Path("/setup-admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @POST
    public Response createAdmin(@Valid CreateAdminRequest createAdminRequest) {

        adminService.setupAdminUser(createAdminRequest);

        return Response.ok().build();
    }
}
