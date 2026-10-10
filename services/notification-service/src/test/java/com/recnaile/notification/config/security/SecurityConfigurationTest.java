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
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_EMPLOYEE".equals(authority.getAuthority())));
    }

    @Test
    void mapsManagingDirectorRoleToRoleMdAuthority() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("employeeId", "EMP001")
                .claim("roles", java.util.List.of("MD"))
                .build();

        AbstractAuthenticationToken authentication =
                configuration.notificationJwtAuthenticationConverter().convert(jwt);

        assertEquals("EMP001", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MD".equals(authority.getAuthority())));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(authority -> "MD".equals(authority.getAuthority())));
    }

    @Test
    void normalizesRolesLikeInventoryServiceWithoutDoublePrefixing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("employeeId", "EMP001")
                .claim("roles", java.util.List.of(" MD ", "ROLE_PROCUREMENT", "", 42))
                .build();

        AbstractAuthenticationToken authentication =
                configuration.notificationJwtAuthenticationConverter().convert(jwt);

        assertEquals("EMP001", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MD".equals(authority.getAuthority())));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PROCUREMENT".equals(authority.getAuthority())));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ROLE_PROCUREMENT".equals(authority.getAuthority())));
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
