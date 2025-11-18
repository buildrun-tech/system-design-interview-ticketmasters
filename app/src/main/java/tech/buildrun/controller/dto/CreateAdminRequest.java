package tech.buildrun.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminRequest(@NotBlank String username,
                                 @NotBlank String password,
                                 @NotBlank String email) {
}
