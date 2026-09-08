package com.recnaile.auth.dto;

import com.recnaile.auth.model.Role;

import java.util.Set;

public record RegisterResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    String departmentId,
    Set<Role> roles,
    boolean enabled
) {
}
