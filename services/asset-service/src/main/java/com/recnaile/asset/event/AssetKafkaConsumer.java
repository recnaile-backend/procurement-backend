package com.recnaile.asset.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes only the business events that belong to the Asset lifecycle.
 *
 * <p>Inventory's StockIssueApproved event is intentionally not consumed here:
 * that event is the Inventory stock-deduction command. Asset creation starts
 * from Outward's OUTWARD_ISSUED event, which contains the recipient, department
 * and per-line assetTracked information needed to build the asset assignment.</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AssetKafkaConsumer {

    private final AssetEventParser parser;
    private final AssetEventHandler handler;

    public AssetKafkaConsumer(AssetEventParser parser, AssetEventHandler handler) {
        this.parser = parser;
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.outward-issued}",
            groupId = "${app.kafka.consumer-group}")
    public void onOutwardIssued(String rawEvent, Acknowledgment acknowledgment) {
        AssetEvent event = parser.parse(rawEvent);
        if (!AssetEventHandler.OUTWARD_ISSUED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unexpected event type on Outward issued topic: " + event.eventType());
        }
        handler.handle(event);
        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "${app.kafka.topics.return-recorded}",
            groupId = "${app.kafka.consumer-group}")
    public void onReturnRecorded(String rawEvent, Acknowledgment acknowledgment) {
        AssetEvent event = parser.parse(rawEvent);
        if (!AssetEventHandler.RETURN_RECORDED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unexpected event type on Return recorded topic: " + event.eventType());
        }
        handler.handle(event);
        acknowledgment.acknowledge();
    }
}
