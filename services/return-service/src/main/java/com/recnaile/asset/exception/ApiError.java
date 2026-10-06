package com.recnaile.asset.exception;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiError", description = "Standard error response returned by the Asset Service")
public record ApiError(
        @Schema(example = "2026-10-06T10:15:30Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "ASSET_VALIDATION_ERROR") String code,
        @Schema(example = "itemId: must not be blank") String message,
        @Schema(example = "/api/v1/assets") String path,
        @Schema(example = "01J8Q8M2J4X2K6Y7Z8A9B0C1D2") String traceId,
        @Schema(example = "4e8c8a42-0e53-4a4d-bb6d-9d5d6e5d0d10") String correlationId) {
}
