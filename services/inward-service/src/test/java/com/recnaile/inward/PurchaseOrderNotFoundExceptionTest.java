package com.recnaile.inward;

import com.recnaile.inward.exception.PurchaseOrderNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseOrderNotFoundExceptionTest {

    @Test
    void containsExactPurchaseOrderIdInMessage() {
        PurchaseOrderNotFoundException exception =
                new PurchaseOrderNotFoundException("P0-009");

        assertEquals("P0-009", exception.getPoId());
        assertEquals("Purchase Order P0-009 was not found", exception.getMessage());
    }
}
