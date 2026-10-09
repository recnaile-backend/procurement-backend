package com.recnaile.returns.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Business return numbers intentionally contain the encoded separator %2F.
     * Swagger encodes the percent sign once more and sends %252F. Allowing the
     * encoded percent and slash at the firewall let Spring Security accept the
     * Swagger path form; ReturnNumberPath then normalizes exactly one extra encoding layer.
     */
    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> {
            StrictHttpFirewall firewall = new StrictHttpFirewall();
            firewall.setAllowUrlEncodedPercent(true);
            firewall.setAllowUrlEncodedSlash(true);
            web.httpFirewall(firewall);
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/actuator/metrics",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/returns").hasAnyRole("TL", "MD", "PROCUREMENT")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.secret:}") String secret,
            @Value("${app.security.jwt.issuer:auth-service}") String issuer,
            @Value("${app.security.jwt.mode:hs256}") String mode,
            @Value("${app.security.jwt.jwk-set-uri:http://auth-service:8081/oauth2/jwks}") String jwkSetUri) {

        /*
         * The local Auth Service signs access tokens with HS256 using
         * the shared JWT_SECRET_KEY. HS256 is the default mode for the
         * local platform environment.
         */
        if ("hs256".equalsIgnoreCase(mode)) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "JWT_SECRET_KEY is required when app.security.jwt.mode=hs256");
            }
            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

            if (secretBytes.length < 32) {
                throw new IllegalStateException(
                        "JWT_SECRET_KEY must be at least 256 bits (32 bytes)");
            }

            SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");

            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withSecretKey(key)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();

            decoder.setJwtValidator(
                    org.springframework.security.oauth2.jwt.JwtValidators
                            .createDefaultWithIssuer(issuer));

            return decoder;
        }

        if ("jwk".equalsIgnoreCase(mode)) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            decoder.setJwtValidator(
                    org.springframework.security.oauth2.jwt.JwtValidators
                            .createDefaultWithIssuer(issuer));
            return decoder;
        }

        throw new IllegalStateException(
                "Unsupported JWT mode: " + mode + ". Use hs256 or jwk");
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof Collection<?> roles) {
                return roles.stream()
                        .map(Object::toString)
                        .map(this::normalizeRole)
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                        .toList();
            }
            Object roleClaim = jwt.getClaims().get("role");
            if (roleClaim != null) {
                return List.of(new SimpleGrantedAuthority(normalizeRole(roleClaim.toString())));
            }
            return List.of();
        });
        return converter;
    }

    private String normalizeRole(String role) {
        String normalized = role.trim();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\"UNAUTHENTICATED\",\"message\":\"Authentication is required\",\"traceId\":null}}");
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"You are not authorized to perform this operation\",\"traceId\":null}}");
        };
    }
}
