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
    @Test
    void nonGlobalRoleWithoutScopeClaimsIsDenied() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
            .claim("roles", List.of("AUDIT"))
            .build();
        assertThrows(AccessDeniedException.class, () -> policy.resolve(jwt, null, null));
    }

    @Test
    void multipleAllowedProjectsRequireAnExplicitProjectFilter() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
            .claim("roles", List.of("TL"))
            .claim("departmentId", "DEPT-7")
            .claim("projectIds", List.of("PROJECT-1", "PROJECT-2"))
            .build();
        assertThrows(AccessDeniedException.class, () -> policy.resolve(jwt, null, null));
        ReportingScopePolicy.Scope scope = policy.resolve(jwt, null, "PROJECT-2");
        assertEquals("DEPT-7", scope.departmentId());
        assertEquals("PROJECT-2", scope.projectId());
    }

    @Test
    void globalReportingRoleCanRequestPlatformWideScope() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
            .claim("roles", List.of("REPORTING"))
            .build();
        ReportingScopePolicy.Scope scope = policy.resolve(jwt, null, null);
        assertTrue(scope.global());
        assertNull(scope.departmentId());
        assertNull(scope.projectId());
    }

}
