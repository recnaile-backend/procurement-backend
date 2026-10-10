package com.recnaile.audit.security;

import com.recnaile.audit.persistence.AuditEventDocument;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuditAccessPolicyTest {
    private final AuditAccessPolicy policy = new AuditAccessPolicy();
    private static Jwt jwt(String role, String subject) {
        return Jwt.withTokenValue("token").header("alg", "HS256")
                .claim("sub", subject).claim("employeeId", subject).claim("roles", List.of(role)).build();
    }
    private static AuditEventDocument event(String actor) {
        return new AuditEventDocument(UUID.randomUUID(), "AUDIT_EVENT", 1, Instant.now(), "indent-service",
                "corr-1", "trace-1", actor, List.of("EMPLOYEE"), "indent-service", "INDENT", "IND-1",
                "CREATE", Instant.now(), null, "CREATED", null, null, null);
    }
    @Test void employeeCanReadOwnActorHistory() { assertDoesNotThrow(() -> policy.requireActorHistoryAccess("u1", jwt("EMPLOYEE", "u1"))); }
    @Test void employeeCannotReadOtherActorHistory() { assertThrows(AccessDeniedException.class, () -> policy.requireActorHistoryAccess("u2", jwt("EMPLOYEE", "u1"))); }
    @Test void procurementAndMdCanReadGlobalAudit() {
        assertDoesNotThrow(() -> policy.requireGlobalAuditRead(jwt("PROCUREMENT", "p1")));
        assertDoesNotThrow(() -> policy.requireGlobalAuditRead(jwt("MD", "m1")));
    }
    @Test void tlCannotReadGlobalAuditWithoutEntityScope() { assertThrows(AccessDeniedException.class, () -> policy.requireGlobalAuditRead(jwt("TL", "tl1"))); }
    @Test void employeeCanReadOnlyOwnEvent() {
        assertDoesNotThrow(() -> policy.requireEventAccess(event("u1"), jwt("EMPLOYEE", "u1")));
        assertThrows(AccessDeniedException.class, () -> policy.requireEventAccess(event("u2"), jwt("EMPLOYEE", "u1")));
    }
    @Test void unknownAdminRoleDoesNotGrantGlobalAccess() { assertThrows(AccessDeniedException.class, () -> policy.requireGlobalAuditRead(jwt("ADMIN", "admin1"))); }
}
