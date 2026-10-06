package com.recnaile.asset.event;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetAssignment;

/**
 * Publishes outbound Asset Service business events.
 *
 * <p>Events are registered for publication after a successful Mongo transaction
 * when a transaction synchronization is active. This prevents a Kafka message
 * from being emitted when the corresponding business mutation is rolled back.
 * Transactional-outbox persistence is intentionally left for the later
 * transactional-outbox phase.</p>
 */
@Service
public class AssetEventPublisher {

    public static final String ASSET_ASSIGNED = "AssetAssigned";
    public static final String ASSET_RETURNED = "AssetReturned";
    public static final String ASSET_STATUS_CHANGED = "AssetStatusChanged";

    private static final Logger log = LoggerFactory.getLogger(AssetEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaEventProperties properties;
    private final AssetServiceProperties serviceProperties;
    private final Clock clock;

    public AssetEventPublisher(
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

    public void publishAssetAssigned(Asset asset, AssetAssignment assignment) {
        Map<String, Object> payload = assignmentPayload(asset, assignment);
        publishAfterCommit(
                properties.topics().assetAssigned(),
                asset.getAssetId(),
                ASSET_ASSIGNED,
                payload);
    }

    public void publishAssetReturned(Asset asset, AssetAssignment assignment) {
        Map<String, Object> payload = assignmentPayload(asset, assignment);
        publishAfterCommit(
                properties.topics().assetReturned(),
                asset.getAssetId(),
                ASSET_RETURNED,
                payload);
    }

    public void publishStatusChanged(
            Asset asset,
            String previousStatus,
            String currentStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getAssetId());
        payload.put("assetReference", asset.getAssetReference());
        payload.put("itemId", asset.getItemId());
        payload.put("issueReference", asset.getIssueReference());
        payload.put("name", asset.getName());
        payload.put("previousStatus", previousStatus);
        payload.put("currentStatus", currentStatus);
        payload.put("condition", asset.getCondition() == null ? null : asset.getCondition().name());
        payload.put("assignedUserId", asset.getAssignedUserId());
        payload.put("departmentName", asset.getDepartmentName());
        payload.put("updatedAt", asset.getUpdatedAt());

        publishAfterCommit(
                properties.topics().assetStatusChanged(),
                asset.getAssetId(),
                ASSET_STATUS_CHANGED,
                payload);
    }

    private Map<String, Object> assignmentPayload(Asset asset, AssetAssignment assignment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getAssetId());
        payload.put("assetReference", asset.getAssetReference());
        payload.put("itemId", asset.getItemId());
        payload.put("issueReference", asset.getIssueReference());
        payload.put("name", asset.getName());
        payload.put("brand", asset.getBrand());
        payload.put("model", asset.getModel());
        payload.put("status", asset.getStatus() == null ? null : asset.getStatus().name());
        payload.put("condition", asset.getCondition() == null ? null : asset.getCondition().name());
        payload.put("assignedUserId", asset.getAssignedUserId());
        payload.put("departmentName", asset.getDepartmentName());
        payload.put("issuedBy", asset.getIssuedBy());
        payload.put("assignmentId", assignment.getAssignmentId());
        payload.put("employeeId", assignment.getEmployeeId());
        payload.put("outwardId", assignment.getOutwardId());
        payload.put("assignedAt", assignment.getAssignedAt());
        payload.put("returnedAt", assignment.getReturnedAt());
        payload.put("returnId", assignment.getReturnId());
        payload.put("returnCondition",
                assignment.getReturnCondition() == null ? null : assignment.getReturnCondition().name());
        payload.put("remarks", assignment.getRemarks());
        return payload;
    }

    private void publishAfterCommit(
            String topic,
            String key,
            String eventType,
            Map<String, Object> payload) {
        if (!properties.enabled()) {
            log.debug("Kafka event publication is disabled; skipping event type={}", eventType);
            return;
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Kafka topic is not configured for event type: " + eventType);
        }

        String eventJson = buildEnvelope(eventType, payload);
        Runnable send = () -> send(topic, key, eventJson);

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

    private String buildEnvelope(String eventType, Map<String, Object> payload) {
        String eventId = UUID.randomUUID().toString();
        String occurredAt = Instant.now(clock).toString();
        String correlationId = firstNonBlank(
                MDC.get("correlationId"),
                MDC.get("traceId"),
                eventId);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("occurredAt", occurredAt);
        envelope.put("source", serviceProperties.serviceName());
        envelope.put("correlationId", correlationId);
        envelope.put("payload", payload);

        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize asset event: " + eventType, ex);
        }
    }

    private void send(String topic, String key, String eventJson) {
        kafkaTemplate.send(topic, key, eventJson)
                .whenComplete(this::handleSendResult);
    }

    private void handleSendResult(SendResult<String, String> result, Throwable error) {
        if (error != null) {
            log.error("Failed to publish Asset Service Kafka event", error);
            return;
        }
        if (result != null && result.getRecordMetadata() != null) {
            log.debug(
                    "Published Asset Service Kafka event topic={} partition={} offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        }
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }
}
