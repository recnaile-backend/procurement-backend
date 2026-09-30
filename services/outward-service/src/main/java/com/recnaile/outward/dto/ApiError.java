package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard application error returned by the Outward Service for business, validation and downstream failures.")
public record ApiError(
        @Schema(description = "Stable machine-readable error code.", example = "OUTWARD_NOT_FOUND")
        String code,
        @Schema(description = "Human-readable explanation of the failure.", example = "Outward request not found")
        String message,
        @Schema(description = "Distributed tracing identifier when available.", example = "4bf92f3577b34da6a3ce929d0e0e4736")
        String traceId,
        @Schema(description = "UTC timestamp when the error response was created.", example = "2026-09-29T10:30:00Z")
        Instant timestamp) {
}
