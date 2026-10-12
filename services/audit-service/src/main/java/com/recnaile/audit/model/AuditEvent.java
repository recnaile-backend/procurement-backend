package com.recnaile.audit.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

/** Typed representation of the platform AUDIT_EVENT envelope. */
public record AuditEvent(
        @NotNull UUID eventId,
        @NotBlank @Pattern(regexp = "AUDIT_EVENT") String eventType,
        @Positive int eventVersion,
        @NotNull Instant occurredAt,
        @NotBlank String producer,
        String correlationId,
        String traceId,
        @NotNull @Valid AuditEventPayload payload
) {
}
