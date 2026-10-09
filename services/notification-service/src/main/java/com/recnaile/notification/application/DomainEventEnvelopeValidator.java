package com.recnaile.notification.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Structural validation for the shared event envelope. Event-specific schemas remain authoritative. */
@Component
public class DomainEventEnvelopeValidator {
    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "INDENT_SUBMITTED", "INDENT_VERIFIED", "INDENT_APPROVED", "INDENT_REJECTED",
            "PO_CREATED", "PO_ISSUED", "PO_CLOSED", "GOODS_RECEIVED", "OUTWARD_ISSUED",
            "RETURN_RECORDED", "LOW_STOCK_ALERT", "SLA_BREACH", "REJECTION_FLAGGED");

    public ValidatedEnvelope validate(JsonNode root) {
        if (root == null || !root.isObject()) throw new IllegalArgumentException("INVALID_EVENT_ENVELOPE");
        String eventId = requiredText(root, "eventId");
        String eventType = requiredText(root, "eventType");
        JsonNode version = root.get("eventVersion");
        if (version == null || !version.canConvertToInt() || version.intValue() < 1)
            throw new IllegalArgumentException("INVALID_EVENT_VERSION");
        String occurredAt = requiredText(root, "occurredAt");
        try { Instant.parse(occurredAt); } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("INVALID_EVENT_TIMESTAMP");
        }
        requiredText(root, "producer");
        JsonNode payload = root.get("payload");
        if (payload == null || !payload.isObject()) throw new IllegalArgumentException("INVALID_EVENT_PAYLOAD");
        if (!SUPPORTED_EVENT_TYPES.contains(eventType)) throw new IllegalArgumentException("UNSUPPORTED_EVENT_TYPE");
        return new ValidatedEnvelope(eventId, eventType, version.intValue());
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank())
            throw new IllegalArgumentException("INVALID_EVENT_" + field.toUpperCase());
        return value.textValue();
    }

    public record ValidatedEnvelope(String eventId, String eventType, int eventVersion) { }
}
