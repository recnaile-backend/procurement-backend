package com.recnaile.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank
    @Size(max = 50)
    String employeeId,

    @NotBlank
    @Size(min = 8, max = 100)
    String password

) {
}