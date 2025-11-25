package tech.buildrun.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.buildrun.controller.dto.CreateAppDto;
import tech.buildrun.controller.dto.CreateAppResponse;

import java.util.Set;

@ApplicationScoped
public class AppService {

    public CreateAppResponse createApp(CreateAppDto dto) {
        // TODO - Implementation for creating an app
        return null;
    }
}
