package com.recnaile.outward.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.outward.domain.model.IssueTransaction;
import com.recnaile.outward.domain.model.OutboxEvent;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.event.OutwardEventEnvelope;
import com.recnaile.outward.event.OutwardEventPayload;
import com.recnaile.outward.repository.OutboxEventRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper,
                         @Value("${app.kafka.events-topic:procurement.events}") String topic) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void enqueueIssued(OutwardRequest outward, IssueTransaction transaction) {
        enqueue("OUTWARD_ISSUED", outward, transaction, null);
    }

    public void enqueueRejected(OutwardRequest outward, String reason) {
        enqueue("OUTWARD_REJECTED", outward, null, reason);
    }

    public void enqueueCancelled(OutwardRequest outward, String reason) {
        enqueue("OUTWARD_CANCELLED", outward, null, reason);
    }

    private void enqueue(String eventType, OutwardRequest outward,
                         IssueTransaction transaction, String reason) {
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();
        String actorId = currentActor();
        String actorRole = currentRole();

        OutwardEventPayload payload = new OutwardEventPayload(
                outward.getOutwardId(),
                transaction == null ? null : transaction.getTransactionId(),
                outward.getItemId(),
                outward.getQuantity(),
                outward.getQuantity(),
                outward.getRecipientUserId(),
                outward.getRecipientUserId(),
                outward.getRecipientName(),
                outward.getRequesterId(),
                outward.getIssuerId(),
                outward.getIssuerId(),
                outward.getApproverId(),
                outward.getDepartmentId(),
                outward.getProjectId(),
                outward.getPurpose(),
                outward.getLocation(),
                outward.isAssetTracked(),
                reason);

        OutwardEventEnvelope envelope = new OutwardEventEnvelope(
                eventId, eventType, 1, now, "outward-service", "OUTWARD",
                outward.getOutwardId(), MDC.get("correlationId"), MDC.get("traceId"),
                actorId, actorRole, 1, payload);

        final String serialized;
        try {
            serialized = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize " + eventType, ex);
        }

        repository.save(new OutboxEvent(
                eventId, "OUTWARD", outward.getOutwardId(), eventType, 1,
                topic, outward.getOutwardId(), serialized, now));
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "SYSTEM";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("SYSTEM");
    }
}
