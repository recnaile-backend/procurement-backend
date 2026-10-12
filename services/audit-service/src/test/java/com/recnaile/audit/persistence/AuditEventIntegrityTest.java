package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.model.AuditEventPayload;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuditEventIntegrityTest {
    @Test void generatedDigestVerifiesForUnmodifiedEvent() {
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, now, "indent-service", null, null,
                new AuditEventPayload("employee-1", List.of("EMPLOYEE"), "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        AuditEventDocument document = AuditEventDocument.from(event);
        assertNotNull(document.integrityHash());
        assertTrue(AuditEventIntegrity.isValid(document));
    }

    @Test void detectsFieldTampering() {
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, now, "indent-service", null, null,
                new AuditEventPayload("employee-1", List.of("EMPLOYEE"), "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        AuditEventDocument original = AuditEventDocument.from(event);
        AuditEventDocument changed = new AuditEventDocument(original.eventId(), original.eventType(), original.eventVersion(), original.occurredAt(), original.producer(), original.correlationId(), original.traceId(), original.actorId(), original.actorRoles(), original.service(), original.entityType(), original.entityId(), "APPROVED", original.timestamp(), original.beforeStatus(), original.afterStatus(), original.poId(), original.deliveryNoteNumber(), original.receivedBy(), original.integrityHash());
        assertFalse(AuditEventIntegrity.isValid(changed));
    }
}
