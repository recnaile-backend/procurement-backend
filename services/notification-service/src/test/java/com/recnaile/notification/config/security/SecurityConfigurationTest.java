package com.recnaile.notification.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void mapsAuthServicesEmployeeIdClaimToCanonicalRecipientPrincipal() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("sub", "internal-user-123")
                .claim("employeeId", "EMP-42")
                .claim("roles", java.util.List.of("EMPLOYEE"))
                .build();

        AbstractAuthenticationToken authentication =
                configuration.notificationJwtAuthenticationConverter().convert(jwt);

        assertEquals("EMP-42", authentication.getName());
    }

    @Test
    void rejectsJwtWithoutEmployeeIdClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("sub", "internal-user-123")
                .build();

        assertTrue(configuration.employeeIdClaimValidator().validate(jwt).hasErrors());
    }
}
