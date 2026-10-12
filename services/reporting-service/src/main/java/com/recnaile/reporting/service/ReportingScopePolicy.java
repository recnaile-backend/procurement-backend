package com.recnaile.reporting.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.List;

/** Applies scope claims when supplied by the platform identity token. */
@Component
public class ReportingScopePolicy {
    public Scope resolve(Jwt jwt, String requestedDepartmentId, String requestedProjectId) {
        List<String> roles=jwt.getClaimAsStringList("roles");
        if(roles==null) roles=jwt.getClaimAsStringList("authorities");
        boolean global=roles!=null && roles.stream().map(r->r.replace("ROLE_", "")).anyMatch(r->r.equals("MD")||r.equals("REPORTING")||r.equals("PROCUREMENT"));
        String tokenDepartment=jwt.getClaimAsString("departmentId");
        String tokenProject=jwt.getClaimAsString("projectId");
        List<String> projectIds=jwt.getClaimAsStringList("projectIds");
        String department=requestedDepartmentId;
        String project=requestedProjectId;
        if(!global && roles!=null && roles.stream().map(r->r.replace("ROLE_", "")).anyMatch("TL"::equals) && (tokenDepartment==null || tokenDepartment.isBlank()) && (tokenProject==null || tokenProject.isBlank()) && (projectIds==null || projectIds.isEmpty())) throw new AccessDeniedException("Team-lead token is missing department/project reporting scope claims");
        if(!global && tokenDepartment!=null && !tokenDepartment.isBlank()) {
            if(department!=null && !department.equals(tokenDepartment)) throw new AccessDeniedException("Requested department is outside the caller's reporting scope");
            department=tokenDepartment;
        }
        if(!global && tokenProject!=null && !tokenProject.isBlank()) {
            if(project!=null && !project.equals(tokenProject)) throw new AccessDeniedException("Requested project is outside the caller's reporting scope");
            project=tokenProject;
        }
        if(!global && projectIds!=null && !projectIds.isEmpty()) {
            if(project!=null && !projectIds.contains(project)) throw new AccessDeniedException("Requested project is outside the caller's reporting scope");
            if(project==null && projectIds.size()==1) project=projectIds.get(0);
        }
        return new Scope(department,project);
    }
    public record Scope(String departmentId,String projectId) {}
}
