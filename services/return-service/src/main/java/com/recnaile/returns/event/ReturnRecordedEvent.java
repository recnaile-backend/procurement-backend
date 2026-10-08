package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory integration event.
 *
 * RETURN_RECORDED is consumed by Inventory Service. The outwardId value is
 * the canonical Outward number (for example OUT%2F26-27%2F001).
 */
public record ReturnRecordedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String returnId,
        String outwardId,
        String itemId,
        Integer qty,
        ReturnCondition condition) {

    public static ReturnRecordedEvent create(ReturnRequest returnRequest) {
        return new ReturnRecordedEvent(
                UUID.randomUUID().toString(),
                "RETURN_RECORDED",
                returnRequest.getUpdatedAt(),
                returnRequest.getReturnId(),
                returnRequest.getOutwardId(),
                returnRequest.getItemId(),
                returnRequest.getQuantity(),
                returnRequest.getCondition());
    }
}
