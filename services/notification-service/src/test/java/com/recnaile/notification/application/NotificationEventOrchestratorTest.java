package com.recnaile.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.notification.application.template.NotificationDraftFactory;
import com.recnaile.notification.application.template.NotificationTemplateCatalog;
import com.recnaile.notification.domain.model.IncomingDomainEvent;
import com.recnaile.notification.domain.model.NotificationDocument;
import com.recnaile.notification.persistence.repository.IncomingDomainEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventOrchestratorTest {
    @Mock NotificationPersistenceService persistence;
    @Mock IncomingDomainEventRepository inboxRepository;
    @Mock EventIdempotencyService idempotencyService;

    private NotificationEventOrchestrator orchestrator() {
        return new NotificationEventOrchestrator(new NotificationDraftFactory(new NotificationTemplateCatalog()),
                persistence, inboxRepository, idempotencyService);
    }

    @Test
    void persistsDraftAndMarksInboxProcessedBeforeReturning() throws Exception {
        var mapper = new ObjectMapper();
        var envelope = mapper.readTree("""
                {"eventId":"evt-1","eventType":"INDENT_SUBMITTED","eventVersion":1,
                 "occurredAt":"2026-10-09T10:00:00Z","producer":"indent-service",
                 "payload":{"empId":"employee-7"}}
                """);
        Map<String, Object> raw = mapper.convertValue(envelope, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        IncomingDomainEvent inbox = new IncomingDomainEvent("evt-1", "INDENT_SUBMITTED", "topic", 0, 1,
                Instant.parse("2026-10-09T10:00:00Z"), raw);
        when(inboxRepository.findByEventId("evt-1")).thenReturn(Optional.of(inbox));
        when(inboxRepository.save(any(IncomingDomainEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(persistence.saveIfAbsent(any(NotificationDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orchestrator().process(envelope, "corr-1", "trace-1");

        assertEquals("evt-1", result.eventId());
        assertEquals(1, result.notificationDraftCount());
        assertTrue(result.processed());
        assertEquals(IncomingDomainEvent.InboxStatus.PROCESSED, inbox.getStatus());
        verify(persistence).saveIfAbsent(any(NotificationDocument.class));
        verify(idempotencyService).markProcessed(eq("evt-1"), any(Instant.class));
    }

    @Test
    void noRecipientCreatesNoNotificationButCompletesEvent() throws Exception {
        var envelope = new ObjectMapper().readTree("""
                {"eventId":"evt-2","eventType":"INDENT_SUBMITTED","payload":{}}
                """);
        Map<String, Object> raw = new LinkedHashMap<>(); raw.put("eventId", "evt-2");
        IncomingDomainEvent inbox = new IncomingDomainEvent("evt-2", "INDENT_SUBMITTED", "topic", 0, 2,
                Instant.now(), raw);
        when(inboxRepository.findByEventId("evt-2")).thenReturn(Optional.of(inbox));
        when(inboxRepository.save(any(IncomingDomainEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orchestrator().process(envelope, null, null);

        assertEquals(0, result.notificationDraftCount());
        verifyNoInteractions(persistence);
        verify(idempotencyService).markProcessed(eq("evt-2"), any(Instant.class));
    }
}
