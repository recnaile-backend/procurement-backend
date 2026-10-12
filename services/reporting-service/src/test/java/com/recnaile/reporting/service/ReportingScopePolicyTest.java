package com.recnaile.reporting.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportingScopePolicyTest {
    private final ReportingScopePolicy policy = new ReportingScopePolicy();

    @Test
    void teamLeadIsRestrictedToTokenDepartmentAndProject() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
            .claim("roles", List.of("TL"))
            .claim("departmentId", "DEPT-7")
            .claim("projectId", "PROJECT-9")
            .build();

        ReportingScopePolicy.Scope scope = policy.resolve(jwt, null, null);
        assertEquals("DEPT-7", scope.departmentId());
        assertEquals("PROJECT-9", scope.projectId());
    }

    @Test
    void teamLeadCannotOverrideDepartmentScope() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
            .claim("roles", List.of("TL"))
            .claim("departmentId", "DEPT-7")
            .build();

        assertThrows(AccessDeniedException.class, () -> policy.resolve(jwt, "DEPT-8", null));
    }

    @Test
    void reportingRoleMayRequestExplicitDepartmentAndProjectFilters() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
            .claim("roles", List.of("REPORTING"))
            .build();

        ReportingScopePolicy.Scope scope = policy.resolve(jwt, "DEPT-2", "PROJECT-3");
        assertEquals("DEPT-2", scope.departmentId());
        assertEquals("PROJECT-3", scope.projectId());
    }
}
