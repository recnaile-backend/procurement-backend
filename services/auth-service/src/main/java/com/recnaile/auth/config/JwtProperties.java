package com.recnaile.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String issuer,
    long accessTokenMinutes,
    Keystore keystore
) {

    public record Keystore(
        String path,
        String alias,
        String password
    ) {
    }
}
