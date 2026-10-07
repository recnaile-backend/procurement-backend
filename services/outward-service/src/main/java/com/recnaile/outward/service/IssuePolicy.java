package com.recnaile.outward.service;

import com.recnaile.outward.exception.OutwardException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/** Endpoint-level authorization for the physical stock issue operation. */
@Component
public class IssuePolicy {

    private final Set<String> allowedRoles;

    public IssuePolicy(@Value("${app.authorization.issue-roles:WAREHOUSE,INVENTORY_OPERATOR,TL,MD}") String roles) {
        this.allowedRoles = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(this::normalizeRole)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new OutwardException("AUTHENTICATION_REQUIRED", "Authenticated user is required");
        }
        boolean allowed = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .anyMatch(allowedRoles::contains);
        if (!allowed) {
            throw new OutwardException("OUTWARD_ISSUE_ROLE_NOT_ALLOWED",
                    "Authenticated role is not configured to issue outward stock");
        }
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
