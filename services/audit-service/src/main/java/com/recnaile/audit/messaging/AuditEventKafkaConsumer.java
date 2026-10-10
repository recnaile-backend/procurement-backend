package com.recnaile.audit.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.audit.model.AuditEvent;
import com.recnaile.audit.application.AuditEventIngestionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Consumes the canonical AUDIT_EVENT envelope and persists it before acknowledging its offset. */
@Component
public class AuditEventKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AuditEventIngestionService ingestionService;

    public AuditEventKafkaConsumer(ObjectMapper objectMapper, Validator validator,
                                   AuditEventIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = "${app.kafka.audit-topic:procurement.audit.events}",
            containerFactory = "auditKafkaListenerContainerFactory")
    public void consume(String rawMessage, Acknowledgment acknowledgment) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("AUDIT_EVENT message must not be blank");
        }
        final AuditEvent event;
        try {
            event = objectMapper.readValue(rawMessage, AuditEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid AUDIT_EVENT JSON envelope", exception);
        }
        if (event == null) {
            throw new IllegalArgumentException("AUDIT_EVENT envelope must not be null");
        }
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("AUDIT_EVENT contract validation failed", violations);
        }
        ingestionService.ingest(event);
        // Never acknowledge until durable MongoDB insertion succeeds.
        acknowledgment.acknowledge();
    }
}
