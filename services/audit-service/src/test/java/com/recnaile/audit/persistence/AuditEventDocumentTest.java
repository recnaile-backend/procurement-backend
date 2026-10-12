package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.model.AuditEventPayload;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventDocumentTest {
    @Test
    void mapsEnvelopeAndPayloadToMongoDocument() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        AuditEventPayload payload = new AuditEventPayload("user-7", List.of("TL"), "indent-service",
            "INDENT", "IND-100", "APPROVED", now, "PENDING", "APPROVED", null, null, null);
        AuditEvent event = new AuditEvent(id, "AUDIT_EVENT", 1, now, "indent-service",
            "corr-1", "trace-1", payload);

        AuditEventDocument doc = AuditEventDocument.from(event);

        assertEquals(id, doc.eventId());
        assertEquals("user-7", doc.actorId());
        assertEquals(List.of("TL"), doc.actorRoles());
        assertEquals("INDENT", doc.entityType());
        assertEquals("IND-100", doc.entityId());
        assertEquals("APPROVED", doc.action());
        assertEquals("PENDING", doc.beforeStatus());
        assertEquals("APPROVED", doc.afterStatus());
        assertEquals("corr-1", doc.correlationId());
        assertEquals("trace-1", doc.traceId());
    }

    @Test
    void rejectsMissingEventOrPayload() {
        assertThrows(IllegalArgumentException.class, () -> AuditEventDocument.from(null));
        AuditEvent incomplete = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, Instant.now(),
            "indent-service", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> AuditEventDocument.from(incomplete));
    }

    @Test
    void declaresAuditCollectionAndQueryIndexes() {
        Document document = AuditEventDocument.class.getAnnotation(Document.class);
        assertNotNull(document);
        assertEquals("audit_events", document.collection());
        CompoundIndexes indexes = AuditEventDocument.class.getAnnotation(CompoundIndexes.class);
        assertNotNull(indexes);
        assertTrue(indexes.value().length >= 5);
        List<String> indexNames = java.util.Arrays.stream(indexes.value())
            .map(org.springframework.data.mongodb.core.index.CompoundIndex::name)
            .toList();
        assertTrue(indexNames.contains("idx_audit_correlation_time"));
        assertTrue(indexNames.contains("idx_audit_trace_time"));
    }
}
