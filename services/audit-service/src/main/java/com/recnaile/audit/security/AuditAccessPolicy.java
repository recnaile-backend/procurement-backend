package com.recnaile.audit.security;

import com.recnaile.audit.persistence.AuditEventDocument;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/** Authorization uses only claims from the cryptographically validated JWT. */
@Component
public class AuditAccessPolicy {
    private static final List<String> GLOBAL_READ_ROLES = List.of("PROCUREMENT", "MD");
    private static final List<String> SELF_READ_ROLES = List.of("EMPLOYEE", "TL");

    public void requireMdOperations(Jwt jwt) {
        if (jwt == null || !hasAnyRole(jwt, List.of("MD"))) {
            throw new AccessDeniedException("Audit operations require MD role");
        }
    }

    public void requireGlobalAuditRead(Jwt jwt) {
        if (jwt == null || !hasAnyRole(jwt, GLOBAL_READ_ROLES)) {
            throw new AccessDeniedException("Global audit access requires PROCUREMENT or MD role");
        }
    }

    public void requireActorHistoryAccess(String actorId, Jwt jwt) {
        if (jwt == null) throw new AccessDeniedException("A validated identity is required");
        if (hasAnyRole(jwt, GLOBAL_READ_ROLES)) return;
        String authenticatedActorId = actorIdentity(jwt);
        if (hasAnyRole(jwt, SELF_READ_ROLES) && actorId != null
                && actorId.equals(authenticatedActorId)) return;
        throw new AccessDeniedException("Actor history is restricted to the authenticated actor");
    }

    public void requireEventAccess(AuditEventDocument event, Jwt jwt) {
        if (jwt == null || event == null) throw new AccessDeniedException("Audit event access denied");
        if (hasAnyRole(jwt, GLOBAL_READ_ROLES)) return;
        String authenticatedActorId = actorIdentity(jwt);
        if (hasAnyRole(jwt, SELF_READ_ROLES) && event.actorId() != null
                && event.actorId().equals(authenticatedActorId)) return;
        throw new AccessDeniedException("Audit event is outside the authenticated actor scope");
    }

    /**
     * Auth Service sets sub to the MongoDB user ID and employeeId to the
     * business identity used by procurement workflows. Audit actorId should
     * use employeeId; sub is a safe compatibility fallback for older events.
     */
    private static String actorIdentity(Jwt jwt) {
        String employeeId = jwt.getClaimAsString("employeeId");
        return employeeId != null && !employeeId.isBlank() ? employeeId : jwt.getSubject();
    }

    private static boolean hasAnyRole(Jwt jwt, List<String> roles) {
        Object raw = jwt.getClaims().get("roles");
        if (!(raw instanceof List<?> claims)) return false;
        for (Object claim : claims) {
            if (claim instanceof String role && roles.contains(role.replaceFirst("^ROLE_", ""))) return true;
        }
        return false;
    }
}
