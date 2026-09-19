package com.recnaile.purchaseorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.purchaseorder.domain.enums.OutboxStatus;
import com.recnaile.purchaseorder.domain.model.OutboxEvent;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.event.PurchaseOrderEventEnvelope;
import com.recnaile.purchaseorder.event.PurchaseOrderEventPayload;
import com.recnaile.purchaseorder.repository.OutboxEventRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PurchaseOrderOutboxService {

    private static final String PRODUCER_SERVICE = "purchase-order-service";
    private static final String AGGREGATE_TYPE = "PURCHASE_ORDER";
    private static final String EVENT_VERSION = "1";
    private static final String SCHEMA_VERSION = "1";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final String topic;

    public PurchaseOrderOutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            Tracer tracer,
            @Value("${app.kafka.purchase-order-events-topic:procurement.purchase-order.events}") String topic) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.topic = topic;
    }

    public void enqueueCreated(PurchaseOrder purchaseOrder, String actorId) {
        enqueue("PO_CREATED", purchaseOrder, actorId);
    }

    public void enqueueIssued(PurchaseOrder purchaseOrder, String actorId) {
        enqueue("PO_ISSUED", purchaseOrder, actorId);
    }

    public void enqueueClosed(PurchaseOrder purchaseOrder, String actorId) {
        enqueue("PO_CLOSED", purchaseOrder, actorId);
    }

    private void enqueue(String eventType, PurchaseOrder purchaseOrder, String actorId) {
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();

        PurchaseOrderEventEnvelope envelope = new PurchaseOrderEventEnvelope(
                eventId,
                eventType,
                EVENT_VERSION,
                now,
                PRODUCER_SERVICE,
                AGGREGATE_TYPE,
                purchaseOrder.getPoId(),
                MDC.get("correlationId"),
                null,
                actorId,
                actorRole(),
                SCHEMA_VERSION,
                PurchaseOrderEventPayload.from(purchaseOrder));

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize Purchase Order outbox event " + eventType, ex);
        }

        OutboxEvent event = new OutboxEvent();
        event.setId("OUTBOX-" + eventId);
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setEventVersion(EVENT_VERSION);
        event.setOccurredAt(now);
        event.setProducerService(PRODUCER_SERVICE);
        event.setAggregateType(AGGREGATE_TYPE);
        event.setAggregateId(purchaseOrder.getPoId());
        event.setActorId(actorId);
        event.setActorRole(actorRole());
        event.setTopic(topic);
        event.setPayload(payload);
        event.setCorrelationId(MDC.get("correlationId"));
        event.setTraceId(traceId());
        event.setStatus(OutboxStatus.PENDING);
        event.setAttemptCount(0);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        repository.save(event);
    }

    private String actorRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }

    private String traceId() {
        if (tracer == null) {
            return null;
        }
        Span span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}
