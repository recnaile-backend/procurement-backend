package com.recnaile.returns.event;

import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;

import java.time.Instant;
import java.util.UUID;

/**
 * Asset integration event.
 *
 * ReturnAccepted is emitted only for asset-tracked/reusable returns.
 * The outwardId value is the canonical Outward number.
 */
public record ReturnAcceptedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String returnId,
        String outwardId,
        String itemId,
        Integer qty,
        ReturnCondition condition) {

    public static ReturnAcceptedEvent create(ReturnRequest returnRequest) {
        return new ReturnAcceptedEvent(
                UUID.randomUUID().toString(),
                "ReturnAccepted",
                returnRequest.getUpdatedAt(),
                returnRequest.getReturnId(),
                returnRequest.getOutwardId(),
                returnRequest.getItemId(),
                returnRequest.getQuantity(),
                returnRequest.getCondition());
    }
}
