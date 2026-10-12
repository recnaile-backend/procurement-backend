package com.recnaile.audit.persistence;

import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.model.AuditEventPayload;
import org.springframework.dao.DuplicateKeyException;
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
    void exactDuplicateReturnsStoredDocumentWithoutOverwriting() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventPersistenceService service = new AuditEventPersistenceService(repository);
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, now, "indent-service",
            "corr-1", "trace-1", new AuditEventPayload("user-1", List.of("EMPLOYEE"),
            "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        AuditEventDocument stored = AuditEventDocument.from(event);
        when(repository.insert(any(AuditEventDocument.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findById(event.eventId())).thenReturn(java.util.Optional.of(stored));

        assertEquals(stored, service.append(event));
        verify(repository, never()).save(any(AuditEventDocument.class));
    }

    @Test
    void sameEventIdWithDifferentContentIsRejected() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventPersistenceService service = new AuditEventPersistenceService(repository);
        Instant now = Instant.parse("2026-10-10T08:00:00Z");
        UUID id = UUID.randomUUID();
        AuditEvent original = new AuditEvent(id, "AUDIT_EVENT", 1, now, "indent-service",
            "corr-1", "trace-1", new AuditEventPayload("user-1", List.of("EMPLOYEE"),
            "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        AuditEvent conflicting = new AuditEvent(id, "AUDIT_EVENT", 1, now, "indent-service",
            "corr-1", "trace-1", new AuditEventPayload("user-2", List.of("EMPLOYEE"),
            "indent-service", "INDENT", "IND-1", "CREATED", now, null, "DRAFT", null, null, null));
        when(repository.insert(any(AuditEventDocument.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findById(id)).thenReturn(java.util.Optional.of(AuditEventDocument.from(original)));

        assertThrows(AuditEventIdentityConflictException.class, () -> service.append(conflicting));
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
