package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.recnaile.outward.exception.OutwardException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
}
