package com.recnaile.asset.event;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.config.AssetServiceProperties;
import com.recnaile.asset.config.KafkaEventProperties;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;

import static org.junit.jupiter.api.Assertions.*;

class AssetEventPublisherTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-06T04:30:00Z"),
            ZoneOffset.UTC);

    @Test
    void publishesAssignedEnvelopeWithStableKafkaKeyAndPayload() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq("asset-assigned"), eq("ASSET%2F26-27%2F001"), anyString()))
                .thenReturn(completedSendResult());

        AssetEventPublisher publisher = new AssetEventPublisher(
                kafkaTemplate,
                new ObjectMapper().findAndRegisterModules(),
                properties(),
                new AssetServiceProperties("asset-service", "test", "assetdb"),
                clock);

        Asset asset = Asset.create(
                new AssetId("ASSET%2F26-27%2F001"),
                "ITEM-1",
                "OUT-1",
                "Laptop",
                "Brand",
                "Model",
                "EMP-ISSUER",
                AssetStatus.IN_USE,
                AssetCondition.GOOD,
                "test",
                clock.instant());

        AssetAssignment assignment = AssetAssignment.start(
                asset.getAssetId(),
                "EMP-1",
                "Engineering",
                "EMP-ISSUER",
                "OUT-1",
                clock.instant(),
                "remarks");

        asset.assignTo("EMP-1", "Engineering", clock.instant());

        publisher.publishAssetAssigned(asset, assignment);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("asset-assigned"), eq(asset.getAssetId()), json.capture());

        var root = new ObjectMapper().readTree(json.getValue());
        assertNotNull(root.get("eventId"));
        assertEquals("AssetAssigned", root.get("eventType").asText());
        assertEquals("asset-service", root.get("source").asText());
        assertEquals("ASSET%2F26-27%2F001", root.path("payload").path("assetId").asText());
        assertEquals("EMP-1", root.path("payload").path("employeeId").asText());
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
                        "asset-status-changed"));

        AssetEventPublisher publisher = new AssetEventPublisher(
                kafkaTemplate,
                new ObjectMapper(),
                disabled,
                new AssetServiceProperties("asset-service", "test", "assetdb"),
                clock);

        publisher.publishStatusChanged(
                mock(Asset.class),
                "RETURNED",
                "RETIRED");

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
                        "asset-status-changed"));
    }

    private static CompletableFuture<SendResult<String, String>> completedSendResult() {
        return CompletableFuture.completedFuture(null);
    }
}
