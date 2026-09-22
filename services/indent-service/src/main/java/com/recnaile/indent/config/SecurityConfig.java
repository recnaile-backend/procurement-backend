package com.recnaile.indent.config;

import com.recnaile.indent.security.JwtRoleConverter;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Main Spring Security filter chain.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /*
                 * REST API using Bearer JWT authentication.
                 */
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Allow Spring Boot error/forward dispatches so that
                         * an underlying application error is not replaced
                         * by a secondary 401 from /error.
                         */
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR,
                                DispatcherType.FORWARD
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        /*
                         * Actuator endpoints.
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/actuator/metrics"
                        ).permitAll()

                        /*
                         * Swagger UI and OpenAPI documentation.
                         */
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/webjars/**"
                        ).permitAll()

                        /*
                         * All business endpoints require authentication.
                         * Method-level @PreAuthorize controls roles.
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * JWT resource server.
                 */
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(
                                jwtAuthenticationEntryPoint()
                        )
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        new JwtRoleConverter()
                                )
                        )
                );

        return http.build();
    }

    /**
     * JWT authentication failure handler.
     *
     * 401 = authentication failed.
     * 403 = authentication succeeded but authorization failed.
     */
    @Bean
    AuthenticationEntryPoint jwtAuthenticationEntryPoint() {

        return (request, response, exception) -> {

            System.err.println(
                    "=================================================="
            );

            System.err.println(
                    "JWT AUTHENTICATION FAILED"
            );

            System.err.println(
                    "Method : " + request.getMethod()
            );

            System.err.println(
                    "URI    : " + request.getRequestURI()
            );

            System.err.println(
                    "Query  : " + request.getQueryString()
            );

            System.err.println(
                    "Reason : " + exception.getMessage()
            );

            System.err.println(
                    "=================================================="
            );

            response.setStatus(401);
            response.setHeader(
                    "WWW-Authenticate",
                    "Bearer"
            );
        };
    }

    /**
     * Keep Spring Security's strict firewall protections.
     *
     * The canonical business value is:
     *
     * IN%2F26-27%2F003
     *
     * When used as a URL path segment, the '%' is URL-encoded and becomes:
     *
     * IN%252F26-27%252F003
     *
     * This is a literal percent-encoded business identifier, not an actual
     * HTTP encoded slash. Therefore encoded-solidus handling is not enabled
     * here.
     */
    @Bean
    HttpFirewall httpFirewall() {

        StrictHttpFirewall firewall =
                new StrictHttpFirewall();

        /*
         * The canonical business identifier is stored literally as:
         *
         *     IN%2F26-27%2F004
         *
         * When used in a URL path, '%' is encoded as '%25', so Swagger sends:
         *
         *     IN%252F26-27%252F004
         *
         * Spring Security must therefore allow encoded percent signs.
         *
         * We intentionally do NOT enable encoded slash (%2F) handling.
         * The slash is part of the literal business value, not an HTTP path
         * separator.
         */
        firewall.setAllowUrlEncodedPercent(true);

        return firewall;
    }

    /**
     * Register the strict firewall with Spring Security.
     */
    @Bean
    WebSecurityCustomizer webSecurityCustomizer(
            HttpFirewall firewall) {

        return web -> web.httpFirewall(firewall);
    }

    /**
     * JWT decoder.
     *
     * Auth Service signs access tokens using HS256, so this service validates
     * the same shared secret and issuer.
     */
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.secret-key}") String secretKey) {

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY is not configured"
            );
        }

        byte[] secretBytes =
                secretKey.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY must be at least 256 bits (32 bytes)"
            );
        }

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(
                                new SecretKeySpec(
                                        secretBytes,
                                        "HmacSHA256"
                                )
                        )
                        .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(
                        issuer
                );

        decoder.setJwtValidator(issuerValidator);

        return decoder;
    }
}
