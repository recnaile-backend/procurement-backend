package com.procurement.platform.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @GetMapping({"/auth","/master-data","/indent","/po","/inward","/inventory","/outward","/returns","/assets","/notifications","/audit","/dashboard"})
    public ResponseEntity<Map<String,Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "code", "SERVICE_UNAVAILABLE",
            "message", "The requested service is temporarily unavailable",
            "timestamp", Instant.now().toString()));
    }
}
