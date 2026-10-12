package com.recnaile.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportingEventContractValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReportingEventContractValidator validator = new ReportingEventContractValidator();

    @Test void acceptsValidIndentSubmittedEnvelope() throws Exception {
        JsonNode event = mapper.readTree("""
            {"eventId":"evt-1","eventType":"INDENT_SUBMITTED","eventVersion":1,
             "occurredAt":"2026-10-12T10:00:00Z","producer":"indent-service",
             "payload":{"indentId":"IND-1","departmentId":"D-1","projectId":"P-1",
             "requestedBy":"U-1","items":[],"estimatedValue":100}}
            """);
        assertDoesNotThrow(() -> validator.validate(event));
    }

    @Test void rejectsMissingEnvelopeField() throws Exception {
        JsonNode event = mapper.readTree("""
            {"eventType":"INDENT_VERIFIED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z",
             "producer":"indent-service","payload":{"indentId":"IND-1","verifiedBy":"U-1"}}
            """);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> validator.validate(event));
        assertTrue(error.getMessage().contains("eventId"));
    }

    @Test void rejectsMissingRequiredPayloadField() throws Exception {
        JsonNode event = mapper.readTree("""
            {"eventId":"evt-2","eventType":"INDENT_VERIFIED","eventVersion":1,
             "occurredAt":"2026-10-12T10:00:00Z","producer":"indent-service",
             "payload":{"indentId":"IND-1"}}
            """);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> validator.validate(event));
        assertTrue(error.getMessage().contains("verifiedBy"));
    }

    @Test void rejectsUnsupportedEventType() throws Exception {
        JsonNode event = mapper.readTree("""
            {"eventId":"evt-3","eventType":"NOT_A_REAL_EVENT","eventVersion":1,
             "occurredAt":"2026-10-12T10:00:00Z","producer":"unknown-service","payload":{}}
            """);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(event));
    }

    @Test void rejectsInvalidTimestamp() throws Exception {
        JsonNode event = mapper.readTree("""
            {"eventId":"evt-4","eventType":"INDENT_VERIFIED","eventVersion":1,
             "occurredAt":"yesterday","producer":"indent-service",
             "payload":{"indentId":"IND-1","verifiedBy":"U-1"}}
            """);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(event));
    }
}
