package com.recnaile.asset.event;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.recnaile.asset.config.AssetServiceProperties;
import com.recnaile.asset.config.KafkaEventProperties;
import com.recnaile.asset.outbox.AssetOutboxService;

class AssetAuditEventPublisherTest {
    @Test
    void enqueuesAuditEvent() {
        AssetOutboxService outbox = mock(AssetOutboxService.class);
        KafkaEventProperties properties = new KafkaEventProperties(true, "asset-service", new KafkaEventProperties.Topics(
                "stock-issue-approved", "return-accepted", "asset-assigned", "asset-returned",
                "asset-status-changed", "audit-events"));
        AssetAuditEventPublisher publisher = new AssetAuditEventPublisher(outbox, properties,
                new AssetServiceProperties("asset-service", "test", "assetdb"),
                Clock.fixed(Instant.parse("2026-10-06T04:30:00Z"), ZoneOffset.UTC));

        publisher.publish("ASSET_CREATED", "ASSET%2F26-27%2F001", null, Map.of("status", "RETURNED"), "EMP-1");

        verify(outbox).enqueue(eq("audit-events"), eq("ASSET%2F26-27%2F001"), eq("AuditEvent"), any(Map.class));
    }
}
