package com.recnaile.returns.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.returns.integration.audit.ReturnAuditEvent;
import com.recnaile.returns.integration.notification.ReturnNotificationEvent;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReturnIntegrationOutboxService {

    private final ReturnIntegrationOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final Clock clock;

    @Autowired
    public ReturnIntegrationOutboxService(
            ReturnIntegrationOutboxRepository repository,
            ObjectMapper objectMapper,
            Environment environment) {
        this(repository, objectMapper, environment, Clock.systemUTC());
    }

    ReturnIntegrationOutboxService(
            ReturnIntegrationOutboxRepository repository,
            ObjectMapper objectMapper,
            Environment environment,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.clock = clock;
    }

    public void audit(
            String returnId,
            String actor,
            String role,
            String action,
            String previousState,
            String newState,
            String correlationId) {
        Instant now = Instant.now(clock);
        String effectiveCorrelationId = effectiveCorrelationId(correlationId);
        ReturnAuditEvent event = new ReturnAuditEvent(
                UUID.randomUUID().toString(),
                "RETURN_AUDIT",
                actor,
                role,
                "RETURN_REQUEST",
                returnId,
                action,
                previousState,
                newState,
                effectiveCorrelationId,
                now);

        enqueue(
                event.eventId(),
                returnId,
                event.eventType(),
                "AUDIT",
                environment.getProperty("app.kafka.topics.audit", "audit.events"),
                event);
    }

    public void notify(
            String returnId,
            String recipient,
            String action,
            String message,
            String correlationId) {
        Instant now = Instant.now(clock);
        String effectiveCorrelationId = effectiveCorrelationId(correlationId);
        ReturnNotificationEvent event = new ReturnNotificationEvent(
                UUID.randomUUID().toString(),
                "RETURN_NOTIFICATION",
                recipient,
                returnId,
                action,
                message,
                effectiveCorrelationId,
                now);

        enqueue(
                event.eventId(),
                returnId,
                event.eventType(),
                "NOTIFICATION",
                environment.getProperty("app.kafka.topics.notification", "notification.events"),
                event);
    }

    private void enqueue(
            String eventId,
            String aggregateId,
            String eventType,
            String target,
            String topic,
            Object event) {
        try {
            repository.save(new ReturnIntegrationOutbox(
                    eventId,
                    aggregateId,
                    eventType,
                    target,
                    topic,
                    objectMapper.writeValueAsString(event),
                    Instant.now(clock)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize integration event",
                    exception);
        }
    }

    private String effectiveCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return MDC.get("correlationId");
    }
}
