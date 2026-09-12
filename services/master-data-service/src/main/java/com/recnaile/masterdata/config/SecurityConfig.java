package com.recnaile.masterdata.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.masterdata.dto.ErrorResponse;

import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Swagger
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Health / monitoring
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/actuator/metrics"
                ).permitAll()

                // CORS preflight
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()

                // Everything else requires JWT
                .anyRequest().authenticated()
            )

            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                    )
                )
            );

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Read roles from Auth Service JWT
        authoritiesConverter.setAuthoritiesClaimName("roles");

        // EMPLOYEE -> ROLE_EMPLOYEE
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {

        return (request, response, authException) -> {

            ErrorResponse errorResponse =
                    new ErrorResponse(
                            Instant.now(),
                            401,
                            "Unauthorized",
                            "Authentication is required or the JWT token is invalid.",
                            request.getRequestURI()
                    );

            response.setStatus(401);
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            objectMapper.writeValue(
                    response.getOutputStream(),
                    errorResponse
            );
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {

        return (request, response, accessDeniedException) -> {

            ErrorResponse errorResponse =
                    new ErrorResponse(
                            Instant.now(),
                            403,
                            "Forbidden",
                            "The authenticated user does not have the required role.",
                            request.getRequestURI()
                    );

            response.setStatus(403);
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            objectMapper.writeValue(
                    response.getOutputStream(),
                    errorResponse
            );
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

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