package com.recnaile.outward.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CancelOutwardRequest")
public record CancelOutwardRequest(
        @Schema(example = "Request withdrawn by employee") String reason
) {}
