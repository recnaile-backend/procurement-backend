package com.recnaile.reporting.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Enforces JWT-bound department/project scope before any reporting query is executed. */
@Component
public class ReportingScopePolicy {
    private static final Set<String> GLOBAL_ROLES = Set.of("MD", "REPORTING", "PROCUREMENT");

    public Scope resolve(Jwt jwt, String requestedDepartmentId, String requestedProjectId) {
        if (jwt == null) throw new AccessDeniedException("A validated JWT is required for reporting access");
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) roles = jwt.getClaimAsStringList("authorities");
        Set<String> normalizedRoles = roles == null ? Set.of() : roles.stream()
            .filter(r -> r != null && !r.isBlank())
            .map(r -> r.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
        boolean global = normalizedRoles.stream().anyMatch(GLOBAL_ROLES::contains);

        String tokenDepartment = clean(jwt.getClaimAsString("departmentName"));
        String tokenProject = clean(jwt.getClaimAsString("projectId"));
        List<String> projectIds = jwt.getClaimAsStringList("projectIds");
        if (projectIds != null) projectIds = projectIds.stream().filter(v -> v != null && !v.isBlank()).distinct().toList();

        if (!global && tokenDepartment == null && tokenProject == null && (projectIds == null || projectIds.isEmpty())) {
            throw new AccessDeniedException("JWT is missing department/project reporting scope claims");
        }

        String department = clean(requestedDepartmentId);
        String project = clean(requestedProjectId);
        if (!global && tokenDepartment != null) {
            if (department != null && !department.equals(tokenDepartment))
                throw new AccessDeniedException("Requested department is outside the caller's reporting scope");
            department = tokenDepartment;
        }
        if (!global && tokenProject != null) {
            if (project != null && !project.equals(tokenProject))
                throw new AccessDeniedException("Requested project is outside the caller's reporting scope");
            project = tokenProject;
        }
        if (!global && projectIds != null && !projectIds.isEmpty()) {
            if (project != null && !projectIds.contains(project))
                throw new AccessDeniedException("Requested project is outside the caller's reporting scope");
            if (project == null && projectIds.size() == 1) project = projectIds.get(0);
            else if (project == null && projectIds.size() > 1)
                throw new AccessDeniedException("Select one project from the caller's allowed projectIds");
        }
        return new Scope(department, project, global);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record Scope(String departmentName, String projectId, boolean global) { }
}
