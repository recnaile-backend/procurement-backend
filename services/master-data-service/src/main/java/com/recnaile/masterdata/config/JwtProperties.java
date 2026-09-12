package com.recnaile.masterdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        String issuer,

        String secretKey

) {
}