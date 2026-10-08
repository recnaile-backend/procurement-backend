package com.recnaile.asset.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.recnaile.asset.config.KafkaEventProperties;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetAssignment;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.outbox.AssetOutboxService;

class AssetEventPublisherTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T04:30:00Z"), ZoneOffset.UTC);

    @Test
    void enqueuesAssignedEventWithoutAssetReference() {
        AssetOutboxService outbox = mock(AssetOutboxService.class);
        KafkaEventProperties properties = properties();
        AssetEventPublisher publisher = new AssetEventPublisher(outbox, properties);
        Asset asset = Asset.create(new AssetId("ASSET%2F26-27%2F001"), "ITEM-1", "OUT/26-27/001",
                "Laptop", "Brand", "Model", "EMP-ISSUER", AssetStatus.IN_USE, AssetCondition.GOOD, "test", clock.instant());
        asset.assignTo("EMP-1", "Engineering", clock.instant());
        AssetAssignment assignment = AssetAssignment.start(asset.getAssetId(), "EMP-1", "Engineering",
                "EMP-ISSUER", "OUT/26-27/001", clock.instant(), "remarks");

        publisher.publishAssetAssigned(asset, assignment);

        verify(outbox).enqueue(eq("asset-assigned"), eq("ASSET%2F26-27%2F001"), eq("AssetAssigned"), any(Map.class));
    }

    private KafkaEventProperties properties() {
        return new KafkaEventProperties(true, "asset-service", new KafkaEventProperties.Topics(
                "stock-issue-approved", "return-accepted", "asset-assigned", "asset-returned",
                "asset-status-changed", "audit-events"));
    }
}
