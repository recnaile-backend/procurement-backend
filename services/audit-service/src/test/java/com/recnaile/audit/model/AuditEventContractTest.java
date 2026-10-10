package com.recnaile.audit.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditEventContractTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void acceptsValidAuditEventEnvelopeAndPayload() {
        assertTrue(validator.validate(validEvent()).isEmpty());
    }

    @Test
    void rejectsWrongEventType() {
        AuditEvent valid = validEvent();
        AuditEvent invalid = new AuditEvent(valid.eventId(), "INDENT_SUBMITTED", valid.eventVersion(),
                valid.occurredAt(), valid.producer(), valid.correlationId(), valid.traceId(), valid.payload());
        assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    void rejectsNonPositiveEventVersion() {
        AuditEvent valid = validEvent();
        AuditEvent invalid = new AuditEvent(valid.eventId(), valid.eventType(), 0,
                valid.occurredAt(), valid.producer(), valid.correlationId(), valid.traceId(), valid.payload());
        assertTrue(validator.validate(invalid).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("eventVersion")));
    }

    @Test
    void rejectsMissingRequiredPayloadFields() {
        AuditEventPayload payload = new AuditEventPayload(" ", null, "", "", "", "", null,
                null, null, null, null, null);
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, Instant.now(),
                "indent-service", null, null, payload);
        assertTrue(validator.validate(event).size() >= 7);
    }

    @Test
    void actorRolesAreDefensivelyCopied() {
        var roles = new java.util.ArrayList<>(List.of("PROCUREMENT"));
        AuditEventPayload payload = new AuditEventPayload("user-1", roles, "indent-service", "INDENT",
                "IND-1", "APPROVE", Instant.parse("2026-10-10T00:00:00Z"), null, "APPROVED",
                null, null, null);
        roles.add("MD");
        assertEquals(List.of("PROCUREMENT"), payload.actorRoles());
    }

    private static AuditEvent validEvent() {
        AuditEventPayload payload = new AuditEventPayload(
                "user-1", List.of("PROCUREMENT"), "indent-service", "INDENT", "IND-2026-0001",
                "APPROVE", Instant.parse("2026-10-10T00:00:00Z"), "VERIFIED", "APPROVED",
                null, null, null);
        return new AuditEvent(UUID.fromString("2cce2e72-9d11-4f14-ae52-5fbe4f2db2a1"),
                "AUDIT_EVENT", 1, Instant.parse("2026-10-10T00:00:01Z"), "indent-service",
                "correlation-123", null, payload);
    }
}
