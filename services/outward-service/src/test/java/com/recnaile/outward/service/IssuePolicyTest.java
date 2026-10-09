package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.recnaile.outward.exception.OutwardException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class IssuePolicyTest {

    private final IssuePolicy policy = new IssuePolicy("WAREHOUSE,INVENTORY_OPERATOR,TL,MD");

    @Test
    void allowsConfiguredIssuerRole() {
        Authentication authentication = auth("warehouse-1", "ROLE_WAREHOUSE");
        assertDoesNotThrow(() -> policy.validate(authentication));
    }

    @Test
    void rejectsUnconfiguredRole() {
        Authentication authentication = auth("employee-1", "ROLE_EMPLOYEE");
        OutwardException exception = assertThrows(OutwardException.class,
                () -> policy.validate(authentication));
        assertEquals("OUTWARD_ISSUE_ROLE_NOT_ALLOWED", exception.getCode());
    }

    private Authentication auth(String name, String role) {
        return new UsernamePasswordAuthenticationToken(name, "N/A",
                List.of(new SimpleGrantedAuthority(role)));
    }
}
