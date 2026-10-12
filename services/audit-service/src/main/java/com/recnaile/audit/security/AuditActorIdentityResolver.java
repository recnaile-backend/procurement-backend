package com.recnaile.audit.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Resolves the business actor identity used by audit records from a validated JWT.
 * Auth Service uses employeeId for the procurement employee identity and sub for
 * the user-document ID. The subject fallback supports older tokens/events.
 */
public final class AuditActorIdentityResolver {
    private AuditActorIdentityResolver() {
    }

    public static String resolve(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object rawEmployeeId = jwt.getClaims().get("employeeId");
        String employeeId = rawEmployeeId instanceof String value ? value : null;
        if (employeeId != null && !employeeId.isBlank()) {
            return employeeId;
        }
        String subject = jwt.getSubject();
        return subject != null && !subject.isBlank() ? subject : null;
    }
}
