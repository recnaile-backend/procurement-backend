package com.recnaile.audit.application;

import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.persistence.AuditEventMongoRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEventQueryServiceTest {
    @Test
    void delegatesEntityHistoryQuery() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventQueryService service = new AuditEventQueryService(repository);
        List<AuditEventDocument> expected = List.of();
        when(repository.findByEntityTypeAndEntityIdOrderByTimestampDesc("INDENT", "IND-1"))
                .thenReturn(expected);
        assertSame(expected, service.findEntityHistory("INDENT", "IND-1"));
    }

    @Test
    void rejectsBlankQueryFieldsBeforeRepositoryCall() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventQueryService service = new AuditEventQueryService(repository);
        assertThrows(IllegalArgumentException.class, () -> service.findActorHistory(" "));
        assertThrows(IllegalArgumentException.class, () -> service.findEntityHistory("INDENT", ""));
        assertThrows(IllegalArgumentException.class, () -> service.findByEventId(null));
        verifyNoInteractions(repository);
    }

    @Test
    void findsEventById() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditEventQueryService service = new AuditEventQueryService(repository);
        UUID eventId = UUID.randomUUID();
        when(repository.findById(eventId)).thenReturn(java.util.Optional.empty());
        assertTrue(service.findByEventId(eventId).isEmpty());
        verify(repository).findById(eventId);
    }
}
