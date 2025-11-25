package tech.buildrun.controller.dto;

import java.util.Set;

public record CreateAppDto(String name, Set<String> scopes) {
}
