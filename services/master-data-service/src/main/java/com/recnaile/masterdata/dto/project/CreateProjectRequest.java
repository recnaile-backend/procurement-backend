package com.recnaile.masterdata.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank
        @Size(max = 100)
        String projectId,

        @NotBlank
        @Size(max = 100)
        String projectName,

        @NotBlank
        String teamLeadId
) {
}
