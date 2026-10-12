package com.recnaile.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

/** Validates the common event envelope and required payload fields from contracts/events. */
@Component
public class ReportingEventContractValidator {
    private static final Map<String, Set<String>> REQUIRED_PAYLOAD_FIELDS = Map.ofEntries(
        Map.entry("ASSET_CLOSURE_REQUESTED", Set.of("assetId", "returnId", "reason")),
        Map.entry("ASSET_CREATION_REQUESTED", Set.of("outwardId", "itemId", "assignedTo")),
        Map.entry("ASSET_STATUS_CHANGED", Set.of("assetId", "previousStatus", "newStatus")),
        Map.entry("AUDIT_EVENT", Set.of("actorId", "actorRoles", "service", "entityType", "entityId", "action", "timestamp")),
        Map.entry("GOODS_RECEIVED", Set.of("grnId", "poId", "itemName", "orderedQuantity", "acceptedQuantity", "rejectedQuantity", "unitOfMeasure", "rejectionReason", "deliveryNoteNumber", "deliveryDate", "receivedAt", "receivedBy", "vendorName")),
        Map.entry("INDENT_APPROVED", Set.of("indentId", "approvedBy", "approverRole")),
        Map.entry("INDENT_REJECTED", Set.of("indentId", "rejectedBy", "reason")),
        Map.entry("INDENT_SUBMITTED", Set.of("indentId", "departmentName", "projectId", "requestedBy", "items", "estimatedValue")),
        Map.entry("INDENT_VERIFIED", Set.of("indentId", "verifiedBy")),
        Map.entry("LOW_STOCK_ALERT", Set.of("itemId", "availableQuantity", "reorderThreshold")),
        Map.entry("OUTWARD_ISSUED", Set.of("outwardId", "itemId", "quantity", "issuedTo", "reusable")),
        Map.entry("PO_CLOSED", Set.of("poId", "closedBy")),
        Map.entry("PO_CREATED", Set.of("poId", "indentId", "vendorId", "items")),
        Map.entry("PO_ISSUED", Set.of("poId", "issuedBy", "issuedAt")),
        Map.entry("REJECTION_FLAGGED", Set.of("grnId", "poId", "itemId", "rejectedQuantity", "reason")),
        Map.entry("RETURN_RECORDED", Set.of("returnId", "itemId", "quantity", "condition")),
        Map.entry("SLA_BREACH", Set.of("entityType", "entityId", "slaWindowHours"))
    );

    private static final Set<String> NULLABLE_GOODS_RECEIVED_FIELDS = Set.of(
        "rejectionReason", "deliveryNoteNumber", "deliveryDate", "receivedAt", "vendorName");

    public void validate(JsonNode envelope) {
        if (envelope == null || !envelope.isObject()) {
            throw new IllegalArgumentException("Event must be a JSON object");
        }
        requireText(envelope, "eventId");
        String eventType = requireText(envelope, "eventType");
        String producer = requireText(envelope, "producer");
        if (!envelope.path("eventVersion").isIntegralNumber() || envelope.path("eventVersion").asInt() < 1) {
            throw new IllegalArgumentException("eventVersion must be an integer greater than or equal to 1");
        }
        String occurredAt = requireText(envelope, "occurredAt");
        try { Instant.parse(occurredAt); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("occurredAt must be an ISO-8601 instant", ex); }
        if (!envelope.path("payload").isObject()) {
            throw new IllegalArgumentException("payload must be a JSON object");
        }
        Set<String> required = REQUIRED_PAYLOAD_FIELDS.get(eventType);
        if (required == null) {
            throw new IllegalArgumentException("Unsupported reporting eventType: " + eventType);
        }
        JsonNode payload = envelope.path("payload");
        for (String field : required) {
            if (!payload.has(field)) {
                throw new IllegalArgumentException("Event " + eventType + " payload is missing required field: " + field);
            }
            JsonNode value = payload.get(field);
            if (value.isNull()) {
                if ("GOODS_RECEIVED".equals(eventType) && NULLABLE_GOODS_RECEIVED_FIELDS.contains(field)) {
                    continue;
                }
                throw new IllegalArgumentException("Event " + eventType + " payload field must not be null: " + field);
            }
            if (value.isTextual() && value.asText().isBlank()) {
                throw new IllegalArgumentException("Event " + eventType + " payload field must not be blank: " + field);
            }
        }
        if ("GOODS_RECEIVED".equals(eventType) && !"inward-service".equals(producer)) {
            throw new IllegalArgumentException("GOODS_RECEIVED producer must be inward-service");
        }
    }

    private static String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required and must be a non-blank string");
        }
        return value.asText();
    }
}
