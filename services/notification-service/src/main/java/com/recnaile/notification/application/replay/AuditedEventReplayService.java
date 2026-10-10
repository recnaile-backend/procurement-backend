package com.recnaile.notification.application.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.notification.application.NotificationEventOrchestrator;
import com.recnaile.notification.domain.model.IncomingDomainEvent;
import com.recnaile.notification.persistence.repository.IncomingDomainEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Replays a durable inbox event only after an audit record has been accepted by Kafka. */
@Service
public class AuditedEventReplayService {
    private final IncomingDomainEventRepository inboxRepository;
    private final NotificationEventOrchestrator orchestrator;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String auditTopic;

    public AuditedEventReplayService(IncomingDomainEventRepository inboxRepository,
            NotificationEventOrchestrator orchestrator, KafkaTemplate<Object, Object> kafkaTemplate,
            ObjectMapper objectMapper, @Value("${app.kafka.audit-topic:procurement.audit.events}") String auditTopic) {
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.orchestrator = Objects.requireNonNull(orchestrator);
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.auditTopic = Objects.requireNonNull(auditTopic);
    }

    public ReplayResult replay(String eventId, String actor) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("eventId must not be blank");
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("actor must not be blank");
        IncomingDomainEvent event = inboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("EVENT_NOT_FOUND"));
        try {
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("eventType", "NotificationEventReplayRequested");
            audit.put("eventId", eventId);
            audit.put("actorEmpId", actor);
            audit.put("occurredAt", Instant.now().toString());
            audit.put("service", "notification-service");
            // Wait for broker acknowledgement before performing the replay. The Audit Service consumes this topic.
            kafkaTemplate.send(auditTopic, objectMapper.writeValueAsString(audit)).get(5, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new IllegalStateException("AUDIT_EVENT_PUBLISH_FAILED", failure);
        }
        JsonNode envelope = objectMapper.valueToTree(event.getEnvelope());
        String correlationId = text(envelope, "correlationId");
        String traceId = text(envelope, "traceId");
        NotificationEventOrchestrator.ProcessingResult result = orchestrator.process(envelope, correlationId, traceId);
        return new ReplayResult(result.eventId(), result.notificationDraftCount(), Instant.now());
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    public record ReplayResult(String eventId, int notificationDraftCount, Instant replayedAt) { }
}
