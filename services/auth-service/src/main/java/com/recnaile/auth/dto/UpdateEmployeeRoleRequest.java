package com.recnaile.auth.dto;

import com.recnaile.auth.model.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRoleRequest(

    @NotNull
    Role role

) {
}