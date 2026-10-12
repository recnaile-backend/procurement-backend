package com.recnaile.audit.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;

class AuditActorIdentityResolverTest {
    @Test void resolvesEmployeeIdWhenPresent() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .claim("sub", "mongo-user").claim("employeeId", "EMP-101").build();
        assertEquals("EMP-101", AuditActorIdentityResolver.resolve(jwt));
    }

    @Test void fallsBackToSubjectForLegacyTokens() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .claim("sub", "mongo-user").build();
        assertEquals("mongo-user", AuditActorIdentityResolver.resolve(jwt));
    }

    @Test void returnsNullForNullJwtOrMissingIdentity() {
        assertNull(AuditActorIdentityResolver.resolve(null));
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .claim("roles", java.util.List.of("EMPLOYEE")).build();
        assertNull(AuditActorIdentityResolver.resolve(jwt));
    }
}
