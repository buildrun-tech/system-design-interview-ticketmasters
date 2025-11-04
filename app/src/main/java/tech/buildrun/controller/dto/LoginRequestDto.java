package tech.buildrun.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String username,
                              @NotBlank String password) {
}
