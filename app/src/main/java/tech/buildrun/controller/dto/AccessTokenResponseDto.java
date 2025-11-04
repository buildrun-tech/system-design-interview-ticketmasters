package tech.buildrun.controller.dto;

public record AccessTokenResponseDto(String accessToken, Long expiresIn) {
}
