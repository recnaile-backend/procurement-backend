package com.recnaile.outward.config;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui.html", "/swagger-ui.html/**", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/webjars/**", "/favicon.ico", "/error",
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/actuator/prometheus", "/actuator/metrics"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/outward/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));
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
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.jwk-set-uri:http://auth-service:8081/oauth2/jwks}") String jwkSetUri,
            @Value("${app.security.jwt.issuer-uri:}") String issuerUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        if (issuerUri != null && !issuerUri.isBlank()) {
            OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
            decoder.setJwtValidator(validator);
        }
        return decoder;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeSecurityError(
                request, response, HttpServletResponse.SC_UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "A valid Bearer access token is required");
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeSecurityError(
                request, response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", "You are not authorized to perform this operation");
    }

    private void writeSecurityError(HttpServletRequest request, HttpServletResponse response,
                                    int status, String code, String message) throws IOException {
        String traceId = request.getHeader("X-Correlation-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = "unknown";
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                + escape(code) + "\",\"message\":\"" + escape(message)
                + "\",\"traceId\":\"" + escape(traceId) + "\"}}");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
