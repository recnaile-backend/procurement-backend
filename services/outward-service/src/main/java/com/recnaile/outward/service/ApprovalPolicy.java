package com.recnaile.outward.service;

import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.exception.OutwardException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPolicy {

    public static final String ERROR_CODE = "OUTWARD_APPROVAL_ROLE_NOT_ALLOWED";
    public static final String DEPARTMENT_REQUIRED_CODE = "OUTWARD_APPROVAL_DEPARTMENT_REQUIRED";
    public static final String DEPARTMENT_MISMATCH_CODE = "OUTWARD_APPROVAL_DEPARTMENT_MISMATCH";

    private final Set<String> allowedRoles;
    private final String departmentClaim;

    @Autowired
    public ApprovalPolicy(
            @Value("${app.authorization.approval-roles:TL,MD}") String roles,
            @Value("${app.authorization.department-claim:departmentName}") String departmentClaim) {
        this.allowedRoles = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(this::normalizeRole)
                .collect(Collectors.toUnmodifiableSet());
        this.departmentClaim = departmentClaim == null || departmentClaim.isBlank()
                ? "departmentName" : departmentClaim.trim();
    }

    /** Backward-compatible constructor for unit tests/default usage. */
    public ApprovalPolicy(String roles) {
        this(roles, "departmentName");
    }

    public void validate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new OutwardException("AUTHENTICATION_REQUIRED", "Authenticated user is required");
        }
        if (!hasAllowedRole(authentication.getAuthorities())) {
            throw new OutwardException(ERROR_CODE,
                    "Authenticated role is not configured to approve outward requests");
        }
    }

    public void validate(Authentication authentication, OutwardRequest request) {
        validate(authentication);

        // MD is a global approver/rejector and is not restricted by department.
        if (hasRole(authentication.getAuthorities(), "MD")) {
            return;
        }

        // TL can approve/reject only requests belonging to the TL's department.
        if (!hasRole(authentication.getAuthorities(), "TL")) {
            return;
        }

        String approverDepartment = extractDepartment(authentication);
        if (approverDepartment == null || approverDepartment.isBlank()) {
            throw new OutwardException(DEPARTMENT_REQUIRED_CODE,
                    "TL department is required to approve or reject outward requests");
        }

        String requestDepartment = request == null ? null : request.getDepartmentName();
        if (requestDepartment == null || requestDepartment.isBlank()
                || !requestDepartment.trim().equalsIgnoreCase(approverDepartment.trim())) {
            throw new OutwardException(DEPARTMENT_MISMATCH_CODE,
                    "TL can approve or reject outward requests only from the same department");
        }
    }

    public void validateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (!hasAllowedRole(authorities)) {
            throw new OutwardException(ERROR_CODE,
                    "Authenticated role is not configured to approve outward requests");
        }
    }

    private boolean hasAllowedRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities != null && authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .anyMatch(allowedRoles::contains);
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        return authorities != null && authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .anyMatch(role::equals);
    }

    private String extractDepartment(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String department = jwt.getClaimAsString(departmentClaim);
            if (department == null || department.isBlank()) {
                department = jwt.getClaimAsString("departmentName");
            }
            if (department == null || department.isBlank()) {
                department = jwt.getClaimAsString("department");
            }
            return department == null ? null : department.trim();
        }
        return null;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}
