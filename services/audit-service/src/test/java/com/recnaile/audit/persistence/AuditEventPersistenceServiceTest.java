package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.model.AuditEventPayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEventPersistenceServiceTest {
    @Test
    void appendsMappedDocumentThroughInsert() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventPersistenceService service = new AuditEventPersistenceService(repository);
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, now, "indent-service",
            "corr-1", "trace-1", new AuditEventPayload("user-1", List.of("EMPLOYEE"),
            "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        AuditEventDocument expected = AuditEventDocument.from(event);
        when(repository.insert(any(AuditEventDocument.class))).thenReturn(expected);

        assertEquals(expected, service.append(event));
        verify(repository).insert(expected);
        verify(repository, never()).save(any(AuditEventDocument.class));
    }

    @Test
    void rejectsNullEventBeforeRepositoryCall() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventPersistenceService service = new AuditEventPersistenceService(repository);
        assertThrows(IllegalArgumentException.class, () -> service.append(null));
        verifyNoInteractions(repository);
    }
}
