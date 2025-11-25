package tech.buildrun.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record CreateUserDto(@NotBlank @Length(min = 3, max = 20) String username,
                            @NotBlank @Email @Length(min = 3, max = 100) String email,
                            @NotBlank @Length(min = 4, max = 16) String password) {
}
