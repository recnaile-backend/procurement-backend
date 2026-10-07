package com.recnaile.inward.exception;

import java.math.BigDecimal;

public class ReceivedQuantityExceedsOrderedException extends IllegalArgumentException {
    public ReceivedQuantityExceedsOrderedException(
            String poId,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal previouslyReceivedQuantity,
            BigDecimal requestedQuantity
    ) {
        super("Received quantity for item " + itemName + " on PO " + poId
                + " exceeds ordered quantity. Ordered=" + orderedQuantity
                + ", previously received=" + previouslyReceivedQuantity
                + ", requested=" + requestedQuantity
                + ", cumulative=" + previouslyReceivedQuantity.add(requestedQuantity));
    }
}
