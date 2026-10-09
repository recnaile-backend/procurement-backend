package com.recnaile.inward;

import com.recnaile.inward.exception.PurchaseOrderNotEligibleException;
import com.recnaile.inward.po.PurchaseOrderClient;
import com.recnaile.inward.po.PurchaseOrderLineSummary;
import com.recnaile.inward.po.PurchaseOrderSummary;
import com.recnaile.inward.service.PurchaseOrderEligibilityService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderEligibilityServiceTest {

    @Test
    void issuedPurchaseOrderIsEligible() {
        PurchaseOrderClient client = poId -> new PurchaseOrderSummary(
                poId,
                "ISSUED",
                "SUP-001",
                List.of(new PurchaseOrderLineSummary(
                        "PO-LINE-001",
                        "Bolt",
                        new BigDecimal("10")
                ))
        );

        PurchaseOrderEligibilityService service =
                new PurchaseOrderEligibilityService(client);

        PurchaseOrderSummary result =
                service.requireIssuedPurchaseOrder("PO-001");

        assertEquals("PO-001", result.id());
        assertTrue(service.isEligible("PO-001"));
    }

    @Test
    void nonIssuedPurchaseOrderIsRejected() {
        PurchaseOrderClient client = poId -> new PurchaseOrderSummary(
                poId,
                "DRAFT",
                "SUP-001",
                List.of()
        );

        PurchaseOrderEligibilityService service =
                new PurchaseOrderEligibilityService(client);

        PurchaseOrderNotEligibleException exception =
                assertThrows(
                        PurchaseOrderNotEligibleException.class,
                        () -> service.requireIssuedPurchaseOrder("PO-002")
                );

        assertEquals("PO-002", exception.getPoId());
        assertEquals("DRAFT", exception.getCurrentStatus());
        assertEquals(
                "Purchase Order PO-002 cannot be used for goods receipt because its current status is DRAFT. The Purchase Order must be ISSUED before a GRN can be created.",
                exception.getMessage()
        );
        assertFalse(service.isEligible("PO-002"));
    }

    @Test
    void blankPoIdIsRejected() {
        PurchaseOrderClient client = poId -> {
            throw new AssertionError("PO client must not be called");
        };

        PurchaseOrderEligibilityService service =
                new PurchaseOrderEligibilityService(client);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.requireIssuedPurchaseOrder(" ")
        );
    }
}
