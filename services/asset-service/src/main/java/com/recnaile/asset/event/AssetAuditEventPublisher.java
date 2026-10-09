package com.recnaile.asset.event;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.recnaile.asset.config.AssetServiceProperties;
import com.recnaile.asset.config.KafkaEventProperties;
import com.recnaile.asset.outbox.AssetOutboxService;

/** Writes durable audit events to the Asset transactional outbox. */
@Service
public class AssetAuditEventPublisher {
    public static final String AUDIT_EVENT = "AuditEvent";

    private final AssetOutboxService outboxService;
    private final KafkaEventProperties properties;
    private final AssetServiceProperties serviceProperties;
    private final Clock clock;

    public AssetAuditEventPublisher(AssetOutboxService outboxService, KafkaEventProperties properties,
            AssetServiceProperties serviceProperties, Clock clock) {
        this.outboxService = outboxService;
        this.properties = properties;
        this.serviceProperties = serviceProperties;
        this.clock = clock;
    }

    public void publish(String action, String entityId, Object before, Object after, String fallbackActorId) {
        if (!properties.enabled()) return;
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
        payload.put("auditEventId", UUID.randomUUID().toString());
        outboxService.enqueue(properties.topics().auditEvent(), entityId, AUDIT_EVENT, payload);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }
}
