package com.recnaile.indent.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(
        Duration ttl
) {
}