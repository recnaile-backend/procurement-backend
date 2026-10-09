package com.recnaile.asset.security;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.recnaile.asset.model.Asset;

/**
 * Entity-level authorization rules for the Asset Service.
 *
 * <p>Authentication is supplied by Spring Security from the validated JWT.
 * Raw identity headers are never used as an authorization source.</p>
 */
@Service
public class AssetAuthorizationService {

    public void requireAnyRole(Authentication authentication, String... roles) {
        requireAuthenticated(authentication);
        if (hasAnyRole(authentication, roles)) {
            return;
        }
        throw new AccessDeniedException("Insufficient role for this asset operation");
    }

    public void requireAssetRead(Authentication authentication, Asset asset) {
        requireAuthenticated(authentication);
        if (hasAnyRole(authentication, "MD", "PROCUREMENT")) {
            return;
        }

        if (hasRole(authentication, "TL")) {
            String department = department(authentication);
            if (department != null && Objects.equals(department, asset.getDepartmentName())) {
                return;
            }
        }

        if (hasRole(authentication, "EMPLOYEE")
                && Objects.equals(subject(authentication), asset.getAssignedUserId())) {
            return;
        }

        throw new AccessDeniedException("You are not authorized to access this asset");
    }

    public void requireAssetMutation(Authentication authentication, Asset asset) {
        requireAuthenticated(authentication);
        if (hasAnyRole(authentication, "MD", "PROCUREMENT")) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to modify this asset");
    }


    public List<Asset> filterReadableAssets(Authentication authentication, List<Asset> assets) {
        requireAuthenticated(authentication);
        if (hasAnyRole(authentication, "MD", "PROCUREMENT")) {
            return assets;
        }
        if (hasRole(authentication, "TL")) {
            String department = department(authentication);
            return assets.stream()
                    .filter(asset -> department != null && Objects.equals(department, asset.getDepartmentName()))
                    .toList();
        }
        String employeeId = subject(authentication);
        return assets.stream()
                .filter(asset -> hasRole(authentication, "EMPLOYEE")
                        && Objects.equals(employeeId, asset.getAssignedUserId()))
                .toList();
    }

    public void requireEmployeeHistory(Authentication authentication, String employeeId) {
        requireAuthenticated(authentication);
        if (hasAnyRole(authentication, "MD", "PROCUREMENT", "TL")) {
            return;
        }
        if (hasRole(authentication, "EMPLOYEE") && Objects.equals(subject(authentication), employeeId)) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to access this employee asset history");
    }

    public void requireDepartmentHistory(Authentication authentication, String departmentName) {
        requireAuthenticated(authentication);
        if (hasAnyRole(authentication, "MD", "PROCUREMENT")) {
            return;
        }
        if ((hasRole(authentication, "TL") || hasRole(authentication, "EMPLOYEE"))
                && Objects.equals(department(authentication), departmentName)) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to access this department asset history");
    }

    public String subject(Authentication authentication) {
        requireAuthenticated(authentication);
        return authentication.getName();
    }

    public String department(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        // JWT-backed Authentication exposes claims through JwtAuthenticationToken.
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuthenticationToken) {
            var jwt = jwtAuthenticationToken.getToken();
            for (String claim : new String[]{"departmentName", "department", "dept"}) {
                String value = jwt.getClaimAsString(claim);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
    }

    private static boolean hasAnyRole(Authentication authentication, String... roles) {
        return Arrays.stream(roles).anyMatch(role -> hasRole(authentication, role));
    }

    private static boolean hasRole(Authentication authentication, String role) {
        String expected = "ROLE_" + role.toUpperCase();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }
}
