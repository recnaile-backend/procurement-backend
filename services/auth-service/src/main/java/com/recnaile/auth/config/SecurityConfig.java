package com.recnaile.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http
    ) throws Exception {

        http
            /*
             * JWT-based API.
             * No browser session/cookie authentication.
             */
            .csrf(csrf -> csrf.disable())

            /*
             * Enable CORS.
             */
            .cors(Customizer.withDefaults())

            /*
             * Stateless authentication.
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            /*
             * Security headers.
             */
            .headers(headers -> headers

                /*
                 * Prevent browsers from interpreting
                 * content as a different MIME type.
                 */
                .contentTypeOptions(Customizer.withDefaults())

                /*
                 * Prevent the application from being
                 * embedded inside a frame.
                 */
                .frameOptions(frame ->
                    frame.deny()
                )

                /*
                 * Control the Referer header.
                 */
                .referrerPolicy(referrer ->
                    referrer.policy(
                        org.springframework.security.web.header.writers
                            .ReferrerPolicyHeaderWriter.ReferrerPolicy
                            .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                    )
                )

                /*
                 * Restrict browser features that this
                 * authentication service does not need.
                 */
                .permissionsPolicy(permissions ->
                    permissions.policy(
                        "camera=(), microphone=(), geolocation=()"
                    )
                )
            )

            /*
             * Authorization rules.
             */
            .authorizeHttpRequests(auth -> auth

                /*
                 * Public authentication endpoints.
                 */
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh"
                ).permitAll()

                /*
                 * Swagger / OpenAPI.
                 */
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                /*
                 * Health and monitoring endpoints.
                 * All actuator endpoints are now public for health checks.
                 */
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/actuator/metrics"
                ).permitAll()

                /*
                 * CORS preflight.
                 */
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()

                /*
                 * Everything else requires authentication.
                 */
                .anyRequest().authenticated()
            )

            /*
             * JWT Resource Server.
             */
            .oauth2ResourceServer(oauth2 ->
                oauth2
                    .jwt(jwt ->
                        jwt.jwtAuthenticationConverter(
                            jwtAuthenticationConverter()
                        )
                    )
                    .authenticationEntryPoint(
                        new HttpStatusEntryPoint(UNAUTHORIZED)
                    )
            )

            /*
             * 401 / 403 handling.
             */
            .exceptionHandling(exception ->
                exception
                    .authenticationEntryPoint(
                        new HttpStatusEntryPoint(UNAUTHORIZED)
                    )
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            response.sendError(
                                FORBIDDEN.value(),
                                "Forbidden"
                            )
                    )
            );

        return http.build();
    }

    /**
     * Converts the JWT "roles" claim into Spring Security
     * authorities.
     *
     * Example:
     *
     * "roles": ["EMPLOYEE"]
     *
     * becomes:
     *
     * ROLE_EMPLOYEE
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        /*
         * Use our custom "roles" claim.
         */
        authoritiesConverter.setAuthoritiesClaimName("roles");

        /*
         * Convert EMPLOYEE -> ROLE_EMPLOYEE.
         */
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
        );

        return converter;
    }

    /**
     * CORS configuration.
     *
     * Development frontend:
     *
     * http://localhost:3000
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
            new CorsConfiguration();

        /*
         * Only allow our frontend.
         */
        configuration.setAllowedOrigins(
            List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type"
            )
        );

        configuration.setExposedHeaders(
            List.of("Authorization")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}