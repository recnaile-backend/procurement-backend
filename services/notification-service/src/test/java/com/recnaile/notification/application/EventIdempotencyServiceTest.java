package com.recnaile.notification.application;

import com.recnaile.notification.domain.model.EventProcessingRecord;
import com.recnaile.notification.persistence.repository.EventProcessingRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventIdempotencyServiceTest {
    private final EventProcessingRecordRepository repository = mock(EventProcessingRecordRepository.class);
    private final EventIdempotencyService service = new EventIdempotencyService(repository);
    private final Instant now = Instant.parse("2026-10-09T10:00:00Z");

    @Test
    void registersNewEvent() {
        when(repository.existsByEventId("event-1")).thenReturn(false);
        when(repository.save(any(EventProcessingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(service.registerIfNew("event-1", "INDENT_SUBMITTED", now));
    }

    @Test
    void ignoresAlreadyRegisteredEvent() {
        when(repository.existsByEventId("event-1")).thenReturn(true);
        assertFalse(service.registerIfNew("event-1", "INDENT_SUBMITTED", now));
        verify(repository, never()).save(any());
    }

    @Test
    void handlesConcurrentDuplicateRegistration() {
        when(repository.existsByEventId("event-1")).thenReturn(false, true);
        when(repository.save(any(EventProcessingRecord.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertFalse(service.registerIfNew("event-1", "INDENT_SUBMITTED", now));
    }

    @Test
    void marksEventProcessed() {
        EventProcessingRecord record = new EventProcessingRecord("event-1", "INDENT_SUBMITTED", now);
        when(repository.findByEventId("event-1")).thenReturn(Optional.of(record));
        when(repository.save(record)).thenReturn(record);
        assertEquals(EventProcessingRecord.ProcessingStatus.PROCESSED, service.markProcessed("event-1", now).getStatus());
    }
}
