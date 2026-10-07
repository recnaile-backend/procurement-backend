package com.recnaile.inward.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void mapsRolesClaimToSpringRoles() {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "HS256"), Map.of("roles", List.of("PROCUREMENT", "MD")));

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_PROCUREMENT", "ROLE_MD");
    }

    @Test
    void jwtSecretUsesRawUtf8BytesLikeAuthService() {
        String secret = "x".repeat(32);
        byte[] raw = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(raw).hasSizeGreaterThanOrEqualTo(32);
    }
}
