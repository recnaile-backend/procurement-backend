package com.recnaile.audit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.model.AuditEventPayload;
import com.recnaile.audit.persistence.AuditEventPersistenceService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuditEventKafkaConsumerTest {
    @Test
    void persistsBeforeAcknowledging() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        Validator validator = mock(Validator.class);
        AuditEventPersistenceService persistence = mock(AuditEventPersistenceService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, Instant.parse("2026-10-10T08:00:00Z"),
                "indent-service", "corr-1", "trace-1",
                new AuditEventPayload("user-1", List.of("EMPLOYEE"),
                        "indent-service", "INDENT", "IND-1", "CREATED",
                        Instant.parse("2026-10-10T08:00:00Z"), null, "DRAFT", null, null, null));
        when(mapper.readValue("{}", AuditEvent.class)).thenReturn(event);
        when(validator.validate(event)).thenReturn(Set.of());
        AuditEventKafkaConsumer consumer = new AuditEventKafkaConsumer(mapper, validator, persistence);

        consumer.consume("{}", acknowledgment);

        verify(persistence).append(event);
        verify(acknowledgment).acknowledge();
        verifyNoMoreInteractions(acknowledgment);
    }

    @Test
    void doesNotAcknowledgeWhenPersistenceFails() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        Validator validator = mock(Validator.class);
        AuditEventPersistenceService persistence = mock(AuditEventPersistenceService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        AuditEvent event = new AuditEvent(UUID.randomUUID(), "AUDIT_EVENT", 1, Instant.parse("2026-10-10T08:00:00Z"),
                "indent-service", "corr-1", "trace-1",
                new AuditEventPayload("user-1", List.of("EMPLOYEE"),
                        "indent-service", "INDENT", "IND-1", "CREATED",
                        Instant.parse("2026-10-10T08:00:00Z"), null, "DRAFT", null, null, null));
        when(mapper.readValue("{}", AuditEvent.class)).thenReturn(event);
        when(validator.validate(event)).thenReturn(Set.of());
        doThrow(new IllegalStateException("Mongo unavailable")).when(persistence).append(event);
        AuditEventKafkaConsumer consumer = new AuditEventKafkaConsumer(mapper, validator, persistence);

        assertThrows(IllegalStateException.class, () -> consumer.consume("{}", acknowledgment));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void rejectsBlankMessageWithoutAcknowledging() {
        AuditEventKafkaConsumer consumer = new AuditEventKafkaConsumer(mock(ObjectMapper.class),
                mock(Validator.class), mock(AuditEventPersistenceService.class));
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        assertThrows(IllegalArgumentException.class, () -> consumer.consume("  ", acknowledgment));
        verifyNoInteractions(acknowledgment);
    }
}
