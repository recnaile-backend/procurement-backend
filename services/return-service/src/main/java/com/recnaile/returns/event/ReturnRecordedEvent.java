package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Kafka business event matching the platform RETURN_RECORDED contract. */
public class ReturnRecordedEvent {

    private final String eventId;
    private final String eventType;
    private final Instant occurredAt;
    private final String returnId;
    private final String itemId;
    private final BigDecimal qty;
    private final ReturnCondition condition;

    public static ReturnRecordedEvent create(ReturnRequest returnRequest) {
        return new ReturnRecordedEvent(
                UUID.randomUUID().toString(),
                "RETURN_RECORDED",
                returnRequest.getUpdatedAt(),
                returnRequest.getReturnId(),
                returnRequest.getItemId(),
                returnRequest.getQuantity(),
                returnRequest.getCondition());
    }

    public ReturnRecordedEvent(
            String returnId,
            String itemId,
            BigDecimal qty,
            ReturnCondition condition,
            Instant occurredAt) {
        this(UUID.randomUUID().toString(), "RETURN_RECORDED", occurredAt,
                returnId, itemId, qty, condition);
    }

    public ReturnRecordedEvent(
            String eventId,
            String eventType,
            Instant occurredAt,
            String returnId,
            String itemId,
            BigDecimal qty,
            ReturnCondition condition) {
        this.eventId = eventId;
        this.eventType = eventType;
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
