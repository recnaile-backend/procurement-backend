package com.recnaile.outward.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CreateOutwardRequest")
public record CreateOutwardRequest(
        @NotBlank @Schema(example = "ITEM-1001") String itemId,
        @Min(value = 1) @Schema(example = "2", minimum = "1") int quantity,
        @NotBlank @Schema(example = "EMP-1007") String recipientUserId,
        @Schema(example = "Rajesh") String recipientName,
        @NotBlank @Schema(example = "DEPT-IT") String departmentId,
        @Schema(example = "PROJ-2001") String projectId,
        @NotBlank @Schema(example = "Laptop issue for project work") String purpose,
        @Schema(example = "RACK-A01") String location
) {}
