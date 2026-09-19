package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.state.PurchaseOrderStateMachine;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Application-facing lifecycle service. Persistence and transport concerns
 * remain outside the state machine.
 */
@Service
public class PurchaseOrderLifecycleService {

    private final Clock clock;

    public PurchaseOrderLifecycleService() {
        this(Clock.systemUTC());
    }

    PurchaseOrderLifecycleService(Clock clock) {
        this.clock = clock;
    }

    public PurchaseOrder transition(PurchaseOrder purchaseOrder,
                                     PurchaseOrderStatus targetStatus) {
        Instant transitionTime = Instant.now(clock);
        PurchaseOrderStateMachine.transition(purchaseOrder, targetStatus, transitionTime);
        return purchaseOrder;
    }
}
