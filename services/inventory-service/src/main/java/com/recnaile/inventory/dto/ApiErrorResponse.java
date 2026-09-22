package com.recnaile.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standard error envelope returned by Inventory Service.")
public record ApiErrorResponse(
        @Schema(example = "false")
        boolean success,

        @Schema(nullable = true)
        Object data,

        @Schema(description = "Error details")
        ErrorDetails error) {

    public record ErrorDetails(
            @Schema(example = "VALIDATION_ERROR")
            String code,

            @Schema(example = "Invalid min/max stock thresholds")
            String message,

            @Schema(example = "2f3d9a7c1b2e4f5a")
            String traceId) {
    }
}
