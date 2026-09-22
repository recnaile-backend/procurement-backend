package com.recnaile.purchaseorder;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.state.PurchaseOrderStateMachine;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderStateMachineTest {

    private static final Instant TIME = Instant.parse("2026-09-19T04:00:00Z");

    @Test
    void allowsDraftToIssued() {
        PurchaseOrder po = order(PurchaseOrderStatus.DRAFT);

        PurchaseOrderStateMachine.transition(po, PurchaseOrderStatus.ISSUED, TIME);

        assertEquals(PurchaseOrderStatus.ISSUED, po.getStatus());
        assertEquals(TIME, po.getIssuedAt());
    }

    @Test
    void allowsIssuedToPartiallyReceived() {
        PurchaseOrder po = order(PurchaseOrderStatus.ISSUED);

        PurchaseOrderStateMachine.transition(po, PurchaseOrderStatus.PARTIALLY_RECEIVED, TIME);

        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, po.getStatus());
        assertEquals(TIME, po.getPartiallyReceivedAt());
    }

    @Test
    void allowsPartialToCompletedAndCompletedIsTerminal() {
        PurchaseOrder po = order(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        PurchaseOrderStateMachine.transition(po, PurchaseOrderStatus.COMPLETED, TIME);

        assertEquals(PurchaseOrderStatus.COMPLETED, po.getStatus());
        assertEquals(TIME, po.getCompletedAt());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> PurchaseOrderStateMachine.transition(po, PurchaseOrderStatus.ISSUED, TIME));

        assertEquals("INVALID_PO_STATE_TRANSITION", ex.getCode());
        assertEquals(PurchaseOrderStatus.COMPLETED, po.getStatus());
    }

    @Test
    void allowsDraftCancellation() {
        PurchaseOrder po = order(PurchaseOrderStatus.DRAFT);

        PurchaseOrderStateMachine.transition(po, PurchaseOrderStatus.CANCELLED, TIME);

        assertEquals(PurchaseOrderStatus.CANCELLED, po.getStatus());
        assertEquals(TIME, po.getCancelledAt());
    }

    @Test
    void rejectsBackwardTransition() {
        PurchaseOrder po = order(PurchaseOrderStatus.ISSUED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> PurchaseOrderStateMachine.transition(po, PurchaseOrderStatus.DRAFT, TIME));

        assertEquals("INVALID_PO_STATE_TRANSITION", ex.getCode());
        assertEquals(PurchaseOrderStatus.ISSUED, po.getStatus());
    }

    private PurchaseOrder order(PurchaseOrderStatus status) {
        PurchaseOrder po = new PurchaseOrder();
        po.transitionStatus(status);
        return po;
    }
}
