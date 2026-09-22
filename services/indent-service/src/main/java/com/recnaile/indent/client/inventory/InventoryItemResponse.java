package com.recnaile.indent.client.inventory;

import java.math.BigDecimal;

/**
 * Minimal Inventory Service response required by the Auto Verify API.
 *
 * availableQuantity is the quantity currently available for issue/use.
 */
public record InventoryItemResponse(
        String itemId,
        String itemName,
        BigDecimal availableQuantity) {
}
