package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.recnaile.outward.exception.OutwardException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ApprovalPolicyTest {

    @Test
    void configuredRoleIsAccepted() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        var authentication = new UsernamePasswordAuthenticationToken("approver", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_TL")));
        assertDoesNotThrow(() -> policy.validate(authentication));
    }

    @Test
    void nonConfiguredRoleIsRejected() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        var authentication = new UsernamePasswordAuthenticationToken("employee", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        assertThrows(OutwardException.class, () -> policy.validate(authentication));
    }
    @Test
    void tlCanApproveSameDepartment() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        var authentication = jwtAuth("TL-1", "ROLE_TL", "Engineering");
        var request = new com.recnaile.outward.domain.model.OutwardRequest();
        request.setDepartmentName("Engineering");
        assertDoesNotThrow(() -> policy.validate(authentication, request));
    }

    @Test
    void tlCannotApproveAnotherDepartment() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        var authentication = jwtAuth("TL-1", "ROLE_TL", "Engineering");
        var request = new com.recnaile.outward.domain.model.OutwardRequest();
        request.setDepartmentName("Finance");
        var ex = assertThrows(OutwardException.class, () -> policy.validate(authentication, request));
        assertEquals(ApprovalPolicy.DEPARTMENT_MISMATCH_CODE, ex.getCode());
    }

    @Test
    void mdCanApproveAnyDepartment() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        var authentication = jwtAuth("MD-1", "ROLE_MD", "Management");
        var request = new com.recnaile.outward.domain.model.OutwardRequest();
        request.setDepartmentName("Finance");
        assertDoesNotThrow(() -> policy.validate(authentication, request));
    }

    private org.springframework.security.core.Authentication jwtAuth(String name, String role, String department) {
        Jwt jwt = new Jwt("token", Instant.EPOCH, Instant.EPOCH.plusSeconds(3600),
                Map.of("alg", "HS256"), Map.of("sub", name, "departmentName", department));
        var authorities = List.of(new SimpleGrantedAuthority(role));
        return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, authorities, name);
    }

}
