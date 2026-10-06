package com.recnaile.asset.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignAssetRequest(
        @NotBlank String employeeId,
        @NotBlank String departmentName,
        @NotBlank String issuedBy,
        @NotBlank String outwardId,
        String remarks) {
}
