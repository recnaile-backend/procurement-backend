package com.recnaile.auth.dto;

import java.time.Instant;
import java.util.Set;

import com.recnaile.auth.model.Role;

public record EmployeeResponse(

    String id,

    String employeeId,

    String firstName,

    String lastName,

    String departmentName,

    Set<Role> roles,

    boolean enabled,

    boolean accountLocked,

    Integer failedLoginAttempts,

    Instant createdAt,

    Instant updatedAt

) {
}