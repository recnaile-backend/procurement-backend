package com.recnaile.outward.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.recnaile.outward.exception.OutwardException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPolicy {

    private final Set<String> allowedRoles;

    public ApprovalPolicy(@Value("${app.authorization.approval-roles:TL,MD}") String roles) {
        this.allowedRoles = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validate(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!allowedRoles.contains(normalized)) {
            throw new OutwardException("APPROVAL_ROLE_NOT_ALLOWED",
                    "Authenticated role is not configured to approve outward requests");
        }
    }
}
