package com.recnaile.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamMemberUpdateRequest(

    @NotBlank
    @Size(max = 50)
    String teamLeaderEmployeeId,

    @Size(max = 100)
    String firstName,

    @Size(max = 100)
    String lastName,

    @Size(max = 100)
    String departmentName

) {
}