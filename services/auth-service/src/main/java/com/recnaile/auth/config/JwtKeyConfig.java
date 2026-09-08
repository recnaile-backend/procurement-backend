package com.recnaile.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeyConfig {

    private final JwtProperties jwtProperties;

    public JwtKeyConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public RSAKey rsaKey() throws Exception {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream inputStream = getKeyStoreInputStream()) {

            keyStore.load(
                inputStream,
                jwtProperties.keystore()
                    .password()
                    .toCharArray()
            );
        }

        KeyStore.PrivateKeyEntry entry =
            (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                jwtProperties.keystore().alias(),
                new KeyStore.PasswordProtection(
                    jwtProperties.keystore()
                        .password()
                        .toCharArray()
                )
            );

        if (entry == null) {
            throw new IllegalStateException(
                "JWT key alias not found: "
                    + jwtProperties.keystore().alias()
            );
        }

        RSAPrivateKey privateKey =
            (RSAPrivateKey) entry.getPrivateKey();

        RSAPublicKey publicKey =
            (RSAPublicKey) entry
                .getCertificate()
                .getPublicKey();

        return new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(jwtProperties.keystore().alias())
            .build();
    }

    private InputStream getKeyStoreInputStream()
        throws Exception {

        String path =
            jwtProperties.keystore().path();

        if (path.startsWith("classpath:")) {

            String classpathLocation =
                path.substring("classpath:".length());

            InputStream inputStream =
                getClass()
                    .getClassLoader()
                    .getResourceAsStream(
                        classpathLocation
                    );

            if (inputStream == null) {
                throw new IllegalStateException(
                    "JWT keystore not found: "
                        + classpathLocation
                );
            }

            return inputStream;
        }

        return new FileInputStream(path);
    }

    /**
     * Creates the JWT encoder used to sign access tokens.
     */
    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {

        JWKSet jwkSet =
            new JWKSet(rsaKey);

        ImmutableJWKSet<SecurityContext> jwkSource =
            new ImmutableJWKSet<>(jwkSet);

        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Creates the JWT decoder used to validate incoming
     * access tokens.
     *
     * Validation includes:
     *
     * 1. RSA signature
     * 2. exp (expiration)
     * 3. nbf (not before), when present
     * 4. issuer (iss)
     */
    @Bean
    public JwtDecoder jwtDecoder(
        RSAKey rsaKey
    ) throws Exception {

        NimbusJwtDecoder decoder =
            NimbusJwtDecoder
                .withPublicKey(
                    rsaKey.toRSAPublicKey()
                )
                .build();

        /*
         * Use Spring Security's standard JWT validators.
         *
         * This validates:
         *
         * - exp
         * - nbf
         * - iss
         */
        decoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(
                jwtProperties.issuer()
            )
        );

        return decoder;
    }
}
