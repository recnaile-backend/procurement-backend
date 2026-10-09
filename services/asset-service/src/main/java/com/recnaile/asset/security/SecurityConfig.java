package com.recnaile.asset.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter,
            ApiSecurityErrorHandler apiSecurityErrorHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/actuator/prometheus", "/actuator/metrics",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(apiSecurityErrorHandler)
                        .accessDeniedHandler(apiSecurityErrorHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(apiSecurityErrorHandler)
                        .accessDeniedHandler(apiSecurityErrorHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.secret:}") String secret,
            @Value("${app.security.jwt.issuer:auth-service}") String issuer,
            @Value("${app.security.jwt.mode:hs256}") String mode,
            @Value("${app.security.jwt.jwk-set-uri:http://auth-service:8081/oauth2/jwks}") String jwkSetUri) {

        /*
         * Local/dev Auth Service access tokens use HS256 with the shared
         * JWT_SECRET_KEY. The same secret must be supplied to this service.
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

            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
            return decoder;
        }

        /*
         * JWK mode remains available for environments where Auth Service
         * uses asymmetric signing such as RS256.
         */
        if ("jwk".equalsIgnoreCase(mode)) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }

        throw new IllegalStateException(
                "Unsupported JWT mode: " + mode + ". Use hs256 or jwk");
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            addRoleClaim(authorities, jwt, "roles");
            addRoleClaim(authorities, jwt, "role");
            return authorities;
        });
        return converter;
    }

    private static void addRoleClaim(Collection<GrantedAuthority> authorities, Jwt jwt, String claimName) {
        Object raw = jwt.getClaims().get(claimName);
        if (raw instanceof Collection<?> values) {
            values.forEach(value -> addRole(authorities, value));
        } else if (raw instanceof String value) {
            for (String role : value.split(",")) {
                addRole(authorities, role);
            }
        }
    }

    private static void addRole(Collection<GrantedAuthority> authorities, Object rawRole) {
        if (rawRole == null) {
            return;
        }
        String role = rawRole.toString().trim();
        if (role.isBlank()) {
            return;
        }
        if (role.startsWith("ROLE_")) {
            authorities.add(() -> role);
        } else {
            authorities.add(() -> "ROLE_" + role.toUpperCase());
        }
    }
}
