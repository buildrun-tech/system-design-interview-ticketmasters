package tech.buildrun.service.strategy;

import tech.buildrun.controller.dto.AccessTokenResponseDto;
import tech.buildrun.controller.dto.LoginRequestDto;

public interface TokenStrategy {

    AccessTokenResponseDto generateToken(String identifier, String secret);
}
