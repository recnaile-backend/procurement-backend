package com.recnaile.asset.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.config.AssetServiceProperties;
import com.recnaile.asset.config.KafkaEventProperties;

@Service
public class AssetOutboxService {
    private final AssetOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaEventProperties properties;
    private final AssetServiceProperties serviceProperties;
    private final Clock clock;

    public AssetOutboxService(AssetOutboxRepository repository, ObjectMapper objectMapper,
            KafkaEventProperties properties, AssetServiceProperties serviceProperties, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.serviceProperties = serviceProperties;
        this.clock = clock;
    }

    public void enqueue(String topic, String key, String eventType, Map<String, Object> payload) {
        if (!properties.enabled()) return;
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Kafka topic is not configured for event type: " + eventType);
        }
        String eventId = UUID.randomUUID().toString();
        String correlationId = firstNonBlank(MDC.get("correlationId"), MDC.get("traceId"), eventId);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("occurredAt", Instant.now(clock).toString());
        envelope.put("source", serviceProperties.serviceName());
        envelope.put("correlationId", correlationId);
        envelope.put("payload", payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            repository.save(AssetOutboxEvent.pending(eventId, topic, key, json, Instant.now(clock)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize asset event: " + eventType, ex);
        }
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }
}
