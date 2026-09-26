package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.*;

import com.recnaile.outward.exception.OutwardException;
import org.junit.jupiter.api.Test;

class ApprovalPolicyTest {

    @Test
    void configuredRoleIsAccepted() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        assertDoesNotThrow(() -> policy.validate("TL"));
        assertDoesNotThrow(() -> policy.validate("md"));
    }

    @Test
    void nonConfiguredRoleIsRejected() {
        ApprovalPolicy policy = new ApprovalPolicy("TL,MD");
        OutwardException ex = assertThrows(OutwardException.class, () -> policy.validate("EMPLOYEE"));
        assertEquals("APPROVAL_ROLE_NOT_ALLOWED", ex.getCode());
    }
}
