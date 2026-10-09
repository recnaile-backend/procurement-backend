package com.recnaile.notification.config.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Validates the same HS256 tokens issued by Auth Service.
 * Auth Service calls the employee identity claim "employeeId"; Notification Service
 * maps that verified claim to its canonical internal recipient field, empId.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    SecretKey notificationJwtSecretKey(@Value("${app.security.jwt.secret-key}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_KEY is not configured");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET_KEY must be at least 32 UTF-8 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder notificationJwtDecoder(
            SecretKey notificationJwtSecretKey,
            @Value("${app.security.jwt.issuer:auth-service}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(notificationJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> standardValidators =
                JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                standardValidators, employeeIdClaimValidator()));
        return decoder;
    }

    @Bean
    SecurityFilterChain notificationSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    OAuth2TokenValidator<Jwt> employeeIdClaimValidator() {
        return jwt -> {
            String employeeId = jwt.getClaimAsString("employeeId");
            if (employeeId == null || employeeId.isBlank()) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "JWT must contain a non-blank employeeId claim", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    @Bean
    JwtAuthenticationConverter notificationJwtAuthenticationConverter() {
        // Keep this as Spring's concrete converter. JWT claim validation belongs
        // in the JwtDecoder validator above; convert(Jwt) is final in this version.
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("employeeId");
        return converter;
    }
}
