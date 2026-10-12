package com.recnaile.audit.application;

import com.recnaile.audit.persistence.AuditEventMongoRepository;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditRetentionServiceTest {
    @Test void disabledRetentionDoesNotQueryOrDeleteAnything() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        AuditRetentionService service = new AuditRetentionService(repository, 0, Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC));
        assertFalse(service.preview().enabled());
        verifyNoInteractions(repository);
    }
    @Test void previewCountsCandidatesWithoutDeleting() {
        AuditEventMongoRepository repository = mock(AuditEventMongoRepository.class);
        Instant now = Instant.parse("2026-10-10T00:00:00Z");
        when(repository.countByTimestampBefore(now.minusSeconds(86400))).thenReturn(3L);
        AuditRetentionService service = new AuditRetentionService(repository, 1, Clock.fixed(now, ZoneOffset.UTC));
        assertEquals(3L, service.preview().candidateCount());
        verify(repository).countByTimestampBefore(now.minusSeconds(86400));
        verifyNoMoreInteractions(repository);
    }
}
