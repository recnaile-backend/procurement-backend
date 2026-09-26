package com.recnaile.outward.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RejectOutwardRequest")
public record RejectOutwardRequest(
        @NotBlank @Schema(example = "Insufficient business justification") String reason
) {}
