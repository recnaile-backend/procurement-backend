package com.recnaile.audit.application;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.persistence.AuditEventPersistenceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEventIngestionServiceTest {
    @Test
    void delegatesValidEventToAppendOnlyPersistence() {
        AuditEventPersistenceService persistence = mock(AuditEventPersistenceService.class);
        AuditEventIngestionService service = new AuditEventIngestionService(persistence);
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, now, "indent-service",
                "corr-1", "trace-1", new com.recnaile.audit.model.AuditEventPayload("user-1", List.of("EMPLOYEE"),
                "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        AuditEventDocument expected = AuditEventDocument.from(event);
        when(persistence.append(event)).thenReturn(expected);
        assertEquals(expected, service.ingest(event));
        verify(persistence).append(event);
    }

    @Test
    void rejectsNullEventBeforeCallingPersistence() {
        AuditEventPersistenceService persistence = mock(AuditEventPersistenceService.class);
        AuditEventIngestionService service = new AuditEventIngestionService(persistence);
        assertThrows(IllegalArgumentException.class, () -> service.ingest(null));
        verifyNoInteractions(persistence);
    }
}
