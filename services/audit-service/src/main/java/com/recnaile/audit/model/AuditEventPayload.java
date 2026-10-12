package com.recnaile.audit.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Explicit business fields defined by the platform AUDIT_EVENT payload schema. */
public record AuditEventPayload(
        @NotBlank String actorId,
        @NotNull List<String> actorRoles,
        @NotBlank String service,
        @NotBlank String entityType,
        @NotBlank String entityId,
        @NotBlank String action,
        @NotNull Instant timestamp,
        String beforeStatus,
        String afterStatus,
        String poId,
        String deliveryNoteNumber,
        String receivedBy
) {
    public AuditEventPayload {
        if (actorRoles != null) {
            actorRoles = Collections.unmodifiableList(new ArrayList<>(actorRoles));
        }
    }
}
