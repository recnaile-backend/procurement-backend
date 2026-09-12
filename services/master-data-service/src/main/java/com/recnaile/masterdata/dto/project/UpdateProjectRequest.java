package com.recnaile.masterdata.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(

        @NotBlank
        @Size(max = 100)
        String projectName,

        @NotBlank
        String teamLeadId,

        Long version
) {
}