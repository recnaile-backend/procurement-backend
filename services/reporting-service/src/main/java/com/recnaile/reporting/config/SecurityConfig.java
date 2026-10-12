package com.recnaile.reporting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain reportingSecurity(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/reporting/dashboard/**").hasAnyRole("PROCUREMENT", "TL", "MD", "REPORTING")
                .requestMatchers("/api/v1/reporting/reports/**").hasAnyRole("PROCUREMENT", "TL", "MD", "REPORTING", "AUDIT")
                .requestMatchers("/api/v1/reporting/admin/**").hasAnyRole("MD", "REPORTING")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) roles = jwt.getClaimAsStringList("authorities");
            if (roles == null) return List.of();
            return roles.stream().filter(r -> r != null && !r.isBlank())
                .map(String::trim).map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new).map(a -> (GrantedAuthority) a).toList();
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.secret-key:${JWT_SECRET_KEY:}}") String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET_KEY is required for HS256 token validation.");
        byte[] keyBytes=secret.getBytes(StandardCharsets.UTF_8);
        if(keyBytes.length<32) throw new IllegalStateException("JWT_SECRET_KEY must contain at least 256 bits (32 bytes).");
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(keyBytes,"HmacSHA256")).build();
    }
}
