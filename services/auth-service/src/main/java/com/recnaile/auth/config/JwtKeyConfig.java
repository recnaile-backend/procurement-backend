package com.recnaile.auth.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtKeyConfig {

    private final JwtProperties jwtProperties;

    public JwtKeyConfig(
        JwtProperties jwtProperties
    ) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public SecretKey jwtSecretKey() {

        if (
            jwtProperties.secretKey() == null ||
            jwtProperties.secretKey().isBlank()
        ) {
            throw new IllegalStateException(
                "JWT_SECRET_KEY is not configured"
            );
        }

        byte[] secretBytes =
            jwtProperties
                .secretKey()
                .getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET_KEY must be at least 256 bits (32 bytes)"
            );
        }

        return new SecretKeySpec(
            secretBytes,
            "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(
        SecretKey jwtSecretKey
    ) {

        return new NimbusJwtEncoder(
            new ImmutableSecret<>(
                jwtSecretKey
            )
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
        SecretKey jwtSecretKey
    ) {

        NimbusJwtDecoder decoder =
            NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .build();

        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(
                jwtProperties.issuer()
            )
        );

        return decoder;
    }
}