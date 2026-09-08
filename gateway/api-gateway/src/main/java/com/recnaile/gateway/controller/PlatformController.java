package com.recnaile.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlatformController {

    @GetMapping("/api/v1/platform/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "service", "api-gateway",
            "status", "UP",
            "timestamp", Instant.now().toString()
        );
    }
}
