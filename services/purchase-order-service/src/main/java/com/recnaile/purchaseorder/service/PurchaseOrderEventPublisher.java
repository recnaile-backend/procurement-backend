package com.recnaile.purchaseorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.event.PurchaseOrderEventEnvelope;
import com.recnaile.purchaseorder.event.PurchaseOrderEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes the externally documented Purchase Order domain events.
 *
 * Phase 10 deliberately keeps the publisher separate from the transactional
 * outbox. Phase 11 will persist the event before publication so a Mongo write
 * and Kafka publication have durable delivery semantics.
 */
@Service
public class PurchaseOrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderEventPublisher.class);
    private static final String PRODUCER_SERVICE = "purchase-order-service";
    private static final String AGGREGATE_TYPE = "PURCHASE_ORDER";
    private static final String EVENT_VERSION = "1";
    private static final String SCHEMA_VERSION = "1";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public PurchaseOrderEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.purchase-order-events-topic:procurement.purchase-order.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publishCreated(PurchaseOrder purchaseOrder, String actorId) {
        publishAfterSuccessfulPersistence("PO_CREATED", purchaseOrder, actorId, null);
    }

    public void publishIssued(PurchaseOrder purchaseOrder, String actorId) {
        publishAfterSuccessfulPersistence("PO_ISSUED", purchaseOrder, actorId, null);
    }

    public void publishClosed(PurchaseOrder purchaseOrder, String actorId) {
        publishAfterSuccessfulPersistence("PO_CLOSED", purchaseOrder, actorId, null);
    }

    private void publishAfterSuccessfulPersistence(
            String eventType,
            PurchaseOrder purchaseOrder,
            String actorId,
            String causationId) {

        Runnable publication = () -> publish(eventType, purchaseOrder, actorId, causationId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publication.run();
                }
            });
        } else {
            // Current Phase 10 persistence is a normal Mongo repository write.
            // Phase 11 will replace this fallback with an atomic outbox path.
            publication.run();
        }
    }

    private void publish(
            String eventType,
            PurchaseOrder purchaseOrder,
            String actorId,
            String causationId) {

        Instant occurredAt = Instant.now();
        PurchaseOrderEventEnvelope event = new PurchaseOrderEventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                EVENT_VERSION,
                occurredAt,
                PRODUCER_SERVICE,
                AGGREGATE_TYPE,
                purchaseOrder.getPoId(),
                correlationId(),
                causationId,
                actorId,
                actorRole(),
                SCHEMA_VERSION,
                PurchaseOrderEventPayload.from(purchaseOrder));

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, purchaseOrder.getPoId(), json)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error(
                                    "Failed to publish purchase-order event: eventId={}, eventType={}, poId={}",
                                    event.eventId(), event.eventType(), purchaseOrder.getPoId(), error);
                        } else {
                            log.debug(
                                    "Published purchase-order event: eventId={}, eventType={}, poId={}, topic={}",
                                    event.eventId(), event.eventType(), purchaseOrder.getPoId(), topic);
                        }
                    });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize Purchase Order event " + eventType, ex);
        }
    }

    private String correlationId() {
        return MDC.get("correlationId");
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
}
