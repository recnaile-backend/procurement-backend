package com.recnaile.auth.dto;

import java.util.Set;

import com.recnaile.auth.model.Role;

public record RegisterResponse(
    String id,
    String employeeId,
    String firstName,
    String lastName,
    String departmentName,
    Set<Role> roles,
    boolean enabled
) {
}