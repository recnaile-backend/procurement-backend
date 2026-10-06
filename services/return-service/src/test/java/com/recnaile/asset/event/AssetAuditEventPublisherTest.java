package com.recnaile.asset.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.MDC;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.config.AssetServiceProperties;
import com.recnaile.asset.config.KafkaEventProperties;

class AssetAuditEventPublisherTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-06T04:30:00Z"),
            ZoneOffset.UTC);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void publishesAuditEnvelopeWithBeforeAfterAndCorrelationMetadata() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq("audit-events"), eq("ASSET%2F26-27%2F001"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        MDC.put("userId", "EMP-1");
        MDC.put("role", "EMPLOYEE");
        MDC.put("correlationId", "corr-123");

        AssetAuditEventPublisher publisher = new AssetAuditEventPublisher(
                kafkaTemplate,
                new ObjectMapper().findAndRegisterModules(),
                properties(),
                new AssetServiceProperties("asset-service", "test", "assetdb"),
                clock);

        publisher.publish(
                "ASSET_STATUS_CHANGED",
                "ASSET%2F26-27%2F001",
                "UNDER_REPAIR",
                "RETURNED",
                "fallback-user");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("audit-events"), eq("ASSET%2F26-27%2F001"), json.capture());

        var root = new ObjectMapper().readTree(json.getValue());
        assertNotNull(root.get("eventId"));
        assertEquals("AuditEvent", root.get("eventType").asText());
        assertEquals("asset-service", root.get("source").asText());
        assertEquals("corr-123", root.get("correlationId").asText());
        assertEquals("EMP-1", root.path("payload").path("actorUserId").asText());
        assertEquals("EMPLOYEE", root.path("payload").path("actorRole").asText());
        assertEquals("ASSET_STATUS_CHANGED", root.path("payload").path("action").asText());
        assertEquals("ASSET", root.path("payload").path("entityType").asText());
        assertEquals("ASSET%2F26-27%2F001", root.path("payload").path("entityId").asText());
        assertEquals("UNDER_REPAIR", root.path("payload").path("before").asText());
        assertEquals("RETURNED", root.path("payload").path("after").asText());
    }

    @Test
    void skipsPublicationWhenKafkaIsDisabled() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        KafkaEventProperties disabled = new KafkaEventProperties(
                false,
                "asset-service",
                new KafkaEventProperties.Topics(
                        "stock-issue-approved",
                        "return-accepted",
                        "asset-assigned",
                        "asset-returned",
                        "asset-status-changed",
                        "audit-events"));

        AssetAuditEventPublisher publisher = new AssetAuditEventPublisher(
                kafkaTemplate,
                new ObjectMapper(),
                disabled,
                new AssetServiceProperties("asset-service", "test", "assetdb"),
                clock);

        publisher.publish("ASSET_CREATED", "ASSET-1", null, "after", "system");

        verifyNoInteractions(kafkaTemplate);
    }

    private KafkaEventProperties properties() {
        return new KafkaEventProperties(
                true,
                "asset-service",
                new KafkaEventProperties.Topics(
                        "stock-issue-approved",
                        "return-accepted",
                        "asset-assigned",
                        "asset-returned",
                        "asset-status-changed",
                        "audit-events"));
    }
}
