package com.recnaile.returns.event;

import com.recnaile.returns.model.enums.ReturnCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka business event emitted after a return is successfully processed.
 * The business payload follows the platform RETURN_RECORDED contract:
 * returnId, itemId, qty and condition.
 */
public class ReturnRecordedEvent {

    private final String eventId;
    private final String eventType;
    private final Instant occurredAt;
    private final String returnId;
    private final String itemId;
    private final BigDecimal qty;
    private final ReturnCondition condition;

    public ReturnRecordedEvent(
            String returnId,
            String itemId,
            BigDecimal qty,
            ReturnCondition condition,
            Instant occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RETURN_RECORDED";
        this.occurredAt = occurredAt;
        this.returnId = returnId;
        this.itemId = itemId;
        this.qty = qty;
        this.condition = condition;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getReturnId() { return returnId; }
    public String getItemId() { return itemId; }
    public BigDecimal getQty() { return qty; }
    public ReturnCondition getCondition() { return condition; }
}
