package com.recnaile.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AddTeamMemberRequest(

    @NotBlank
    String employeeId,

    String teamLeaderEmployeeId

) {
}