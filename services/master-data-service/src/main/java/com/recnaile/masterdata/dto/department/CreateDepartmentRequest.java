package com.recnaile.masterdata.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description
) {
}