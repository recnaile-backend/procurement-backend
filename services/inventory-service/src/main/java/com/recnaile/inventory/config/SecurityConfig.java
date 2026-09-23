package com.recnaile.inventory.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /*
     * Swagger/OpenAPI, health, and metrics endpoints are intentionally public.
     * Business APIs remain protected by JWT + RBAC below.
     *
     * Swagger UI loads several resources after the initial /swagger-ui.html
     * request, so all Swagger UI/static resource paths must be permitted.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
        "/swagger-ui.html",
        "/swagger-ui.html/**",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml",
        "/webjars/**",
        "/favicon.ico",
        "/error",
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info",
        "/actuator/prometheus"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())

            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(
                    org.springframework.security.web.header.writers
                        .ReferrerPolicyHeaderWriter.ReferrerPolicy
                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions.policy(
                    "camera=(), microphone=(), geolocation=()")))

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // Swagger/OpenAPI + public operational endpoints.
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                // Browser pre-flight requests.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Inventory read APIs.
                .requestMatchers(HttpMethod.GET, "/api/v1/inventory/**")
                    .hasAnyRole("EMPLOYEE", "PROCUREMENT", "TL", "MD")

                // Inventory threshold administration.
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/v1/inventory/stock/*/thresholds",
                    "/api/v1/inventory/stock/*/available-quantity")
                    .hasAnyRole("PROCUREMENT", "MD")

                // Everything else must be authenticated.
                .anyRequest().authenticated())

            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractRoles);
        return converter;
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Object rawRoles = jwt.getClaims().get("roles");

        if (!(rawRoles instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .filter(role -> !role.isBlank())
            .map(role -> role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .map(authority -> (GrantedAuthority) authority)
            .toList();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${JWT_SECRET_KEY:}") String jwtSecret,
            @Value("${app.security.jwt.issuer-uri:auth-service}") String issuerUri) {

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is required. Configure JWT_SECRET_KEY for token validation.");
        }

        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET_KEY must be at least 256 bits (32 bytes).");
        }

        SecretKey secretKey =
            new SecretKeySpec(secretBytes, "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        OAuth2TokenValidator<Jwt> validator =
            JwtValidators.createDefaultWithIssuer(issuerUri);

        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
            writeSecurityError(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "A valid Bearer access token is required");
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
            writeSecurityError(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED",
                "You are not authorized to perform this operation");
    }

    private void writeSecurityError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {

        String traceId = request.getHeader("X-Correlation-Id");

        if (traceId == null || traceId.isBlank()) {
            traceId = "unknown";
        }

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"success\":false,\"data\":null,\"error\":{" +
            "\"code\":\"" + escape(code) + "\"," +
            "\"message\":\"" + escape(message) + "\"," +
            "\"traceId\":\"" + escape(traceId) + "\"}}");
    }

    private String escape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
