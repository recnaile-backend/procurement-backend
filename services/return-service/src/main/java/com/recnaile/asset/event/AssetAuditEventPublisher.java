package com.recnaile.asset.event;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.config.AssetServiceProperties;
import com.recnaile.asset.config.KafkaEventProperties;

/**
 * Publishes the generic audit contract from Asset Service.
 * Audit persistence remains the responsibility of Audit Service.
 */
@Service
public class AssetAuditEventPublisher {

    public static final String AUDIT_EVENT = "AuditEvent";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaEventProperties properties;
    private final AssetServiceProperties serviceProperties;
    private final Clock clock;

    public AssetAuditEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaEventProperties properties,
            AssetServiceProperties serviceProperties,
            Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.serviceProperties = serviceProperties;
        this.clock = clock;
    }

    public void publish(
            String action,
            String entityId,
            Object before,
            Object after,
            String fallbackActorId) {
        if (!properties.enabled()) {
            return;
        }
        String topic = properties.topics().auditEvent();
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Kafka audit event topic is not configured");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("actorUserId", firstNonBlank(MDC.get("userId"), fallbackActorId, "system"));
        payload.put("actorRole", firstNonBlank(MDC.get("role"), "SYSTEM", "SYSTEM"));
        payload.put("service", serviceProperties.serviceName());
        payload.put("entityType", "ASSET");
        payload.put("entityId", entityId);
        payload.put("action", action);
        payload.put("timestamp", Instant.now(clock).toString());
        payload.put("before", before);
        payload.put("after", after);

        String eventId = UUID.randomUUID().toString();
        String correlationId = firstNonBlank(
                MDC.get("correlationId"),
                MDC.get("traceId"),
                eventId);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", AUDIT_EVENT);
        envelope.put("occurredAt", Instant.now(clock).toString());
        envelope.put("source", serviceProperties.serviceName());
        envelope.put("correlationId", correlationId);
        envelope.put("payload", payload);

        final String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize asset audit event: " + action, ex);
        }

        Runnable send = () -> kafkaTemplate.send(topic, entityId, eventJson)
                .whenComplete(this::handleSendResult);

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void handleSendResult(SendResult<String, String> result, Throwable error) {
        if (error != null) {
            org.slf4j.LoggerFactory.getLogger(AssetAuditEventPublisher.class)
                    .error("Failed to publish Asset Service audit event", error);
        }
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }
}
