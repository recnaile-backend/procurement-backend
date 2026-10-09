package com.recnaile.outward.outbox;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Existing Inventory contract for a stock issue. One event represents one stock
 * line so Inventory can atomically validate and deduct that line exactly once.
 * The event is emitted by Outward only after the approved request is actually issued.
 */
public record StockIssueApprovedEventPayload(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String outwardId,
        String itemId,
        BigDecimal qty,
        String userId,
        String approvedBy,
        boolean reusable) {
}
