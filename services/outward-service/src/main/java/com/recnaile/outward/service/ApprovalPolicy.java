package com.recnaile.outward.service;

import com.recnaile.outward.exception.OutwardException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPolicy {

    public static final String ERROR_CODE = "OUTWARD_APPROVAL_ROLE_NOT_ALLOWED";

    private final Set<String> allowedRoles;

    public ApprovalPolicy(@Value("${app.authorization.approval-roles:TL,MD}") String roles) {
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
            throw new OutwardException(ERROR_CODE,
                    "Authenticated role is not configured to approve outward requests");
        }
    }

    public void validateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        boolean allowed = authorities != null && authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .anyMatch(allowedRoles::contains);
        if (!allowed) {
            throw new OutwardException(ERROR_CODE,
                    "Authenticated role is not configured to approve outward requests");
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
