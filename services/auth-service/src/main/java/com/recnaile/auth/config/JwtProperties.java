package com.recnaile.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String issuer,
    long accessTokenMinutes,
    String secretKey
) {
}