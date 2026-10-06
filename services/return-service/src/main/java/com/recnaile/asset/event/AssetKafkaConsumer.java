package com.recnaile.asset.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

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
            topics = "${app.kafka.topics.stock-issue-approved}",
            groupId = "${app.kafka.consumer-group}")
    public void onStockIssueApproved(String rawEvent, Acknowledgment acknowledgment) {
        AssetEvent event = parser.parse(rawEvent);
        if (!AssetEventHandler.STOCK_ISSUE_APPROVED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unexpected event type on StockIssueApproved topic: " + event.eventType());
        }
        handler.handle(event);
        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "${app.kafka.topics.return-accepted}",
            groupId = "${app.kafka.consumer-group}")
    public void onReturnAccepted(String rawEvent, Acknowledgment acknowledgment) {
        AssetEvent event = parser.parse(rawEvent);
        if (!AssetEventHandler.RETURN_ACCEPTED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unexpected event type on ReturnAccepted topic: " + event.eventType());
        }
        handler.handle(event);
        acknowledgment.acknowledge();
    }
}
