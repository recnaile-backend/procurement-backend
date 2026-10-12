package com.recnaile.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.audit.model.AuditEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Validates an operator-supplied failed envelope before republishing it to the source topic. */
@Service
public class AuditEventReplayService {
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public AuditEventReplayService(ObjectMapper objectMapper, Validator validator,
            @org.springframework.beans.factory.annotation.Qualifier("auditDltKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.audit-topic:procurement.audit.events}") String topic) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public ReplayReceipt replay(String rawEventJson) {
        if (rawEventJson == null || rawEventJson.isBlank()) {
            throw new IllegalArgumentException("rawEventJson must not be blank");
        }
        final AuditEvent event;
        try {
            event = objectMapper.readValue(rawEventJson, AuditEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed event is not valid AUDIT_EVENT JSON", exception);
        }
        Set<ConstraintViolation<AuditEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Failed event does not satisfy the AUDIT_EVENT contract");
        }
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), rawEventJson).get(10, TimeUnit.SECONDS);
            return new ReplayReceipt(event.eventId().toString(), topic, "PUBLISHED",
                    "Validated event was republished; normal idempotent ingestion applies.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Replay interrupted before broker confirmation", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka did not confirm the replay publication", exception);
        }
    }

    public record ReplayReceipt(String eventId, String topic, String status, String message) { }
}
