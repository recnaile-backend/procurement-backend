package com.recnaile.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(

    @NotBlank
    @Size(max = 50)
    String employeeId,

    @NotBlank
    @Size(max = 50)
    String teamLeaderEmployeeId,

    @NotBlank
    @Size(max = 100)
    String firstName,

    @NotBlank
    @Size(max = 100)
    String lastName,

    @NotBlank
    @Size(max = 100)
    String departmentName

) {
}