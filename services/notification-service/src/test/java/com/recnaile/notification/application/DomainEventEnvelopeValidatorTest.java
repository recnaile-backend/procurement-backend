package com.recnaile.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventEnvelopeValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final DomainEventEnvelopeValidator validator = new DomainEventEnvelopeValidator();

    @Test void acceptsSupportedEnvelope() throws Exception {
        var root = mapper.readTree("""
            {"eventId":"evt-1","eventType":"INDENT_SUBMITTED","eventVersion":1,
             "occurredAt":"2026-10-09T10:00:00Z","producer":"indent-service","payload":{"indentId":"I-1"}}
            """);
        var result = validator.validate(root);
        assertEquals("evt-1", result.eventId());
        assertEquals("INDENT_SUBMITTED", result.eventType());
    }

    @Test void rejectsUnsupportedEventType() throws Exception {
        var root = mapper.readTree("""
            {"eventId":"evt-1","eventType":"UNKNOWN_EVENT","eventVersion":1,
             "occurredAt":"2026-10-09T10:00:00Z","producer":"x","payload":{}}
            """);
        assertEquals("UNSUPPORTED_EVENT_TYPE", assertThrows(IllegalArgumentException.class,
                () -> validator.validate(root)).getMessage());
    }

    @Test void rejectsMissingPayload() throws Exception {
        var root = mapper.readTree("""
            {"eventId":"evt-1","eventType":"INDENT_SUBMITTED","eventVersion":1,
             "occurredAt":"2026-10-09T10:00:00Z","producer":"x"}
            """);
        assertEquals("INVALID_EVENT_PAYLOAD", assertThrows(IllegalArgumentException.class,
                () -> validator.validate(root)).getMessage());
    }
}
