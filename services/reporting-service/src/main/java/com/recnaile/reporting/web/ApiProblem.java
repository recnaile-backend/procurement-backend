package com.recnaile.reporting.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;

/** Stable RFC 7807-style error body returned by reporting HTTP endpoints. */
@Schema(name = "ApiProblem", description = "Standard reporting API error response")
public record ApiProblem(
        @Schema(description = "Problem type URI", example = "about:blank") URI type,
        @Schema(description = "Short human-readable summary", example = "Invalid reporting request") String title,
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "Safe explanation of the failure", example = "Unsupported sort field: internalField") String detail,
        @Schema(description = "Request path that produced the error", example = "/api/v1/reporting/reports/events") URI instance,
        @Schema(description = "Stable machine-readable error code", example = "INVALID_REQUEST") String code,
        @Schema(description = "Request correlation identifier; send X-Correlation-ID to trace the request", example = "b87f2c2a-4e93-4cb6-a07c-8f99fbc4e3a0") String correlationId) {
}
