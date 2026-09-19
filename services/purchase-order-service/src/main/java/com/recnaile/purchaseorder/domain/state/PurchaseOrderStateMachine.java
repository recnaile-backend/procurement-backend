package com.recnaile.purchaseorder.domain.state;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.exception.BusinessRuleException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative Purchase Order lifecycle rules.
 *
 * Status changes must pass through this state machine; callers must not
 * arbitrarily mutate the status field.
 */
public final class PurchaseOrderStateMachine {

    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> ALLOWED =
            buildTransitions();

    private PurchaseOrderStateMachine() {
    }

    public static void transition(PurchaseOrder purchaseOrder,
                                  PurchaseOrderStatus targetStatus,
                                  Instant transitionTime) {
        if (purchaseOrder == null) {
            throw new IllegalArgumentException("purchaseOrder must not be null");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("targetStatus must not be null");
        }
        if (transitionTime == null) {
            throw new IllegalArgumentException("transitionTime must not be null");
        }

        PurchaseOrderStatus currentStatus = purchaseOrder.getStatus();
        Set<PurchaseOrderStatus> allowedTargets = ALLOWED.getOrDefault(
                currentStatus, EnumSet.noneOf(PurchaseOrderStatus.class));

        if (!allowedTargets.contains(targetStatus)) {
            throw new BusinessRuleException(
                    "INVALID_PO_STATE_TRANSITION",
                    "Purchase Order cannot transition from " + currentStatus + " to " + targetStatus);
        }

        applyTransitionTimestamp(purchaseOrder, targetStatus, transitionTime);
    }

    public static boolean isAllowed(PurchaseOrderStatus currentStatus,
                                    PurchaseOrderStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        return ALLOWED.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public static Set<PurchaseOrderStatus> allowedTransitionsFrom(PurchaseOrderStatus currentStatus) {
        return Set.copyOf(ALLOWED.getOrDefault(currentStatus, Set.of()));
    }

    private static void applyTransitionTimestamp(PurchaseOrder purchaseOrder,
                                                  PurchaseOrderStatus targetStatus,
                                                  Instant transitionTime) {
        switch (targetStatus) {
            case ISSUED -> purchaseOrder.setIssuedAt(transitionTime);
            case PARTIALLY_RECEIVED -> purchaseOrder.setPartiallyReceivedAt(transitionTime);
            case COMPLETED -> purchaseOrder.setCompletedAt(transitionTime);
            case CLOSED -> purchaseOrder.setClosedAt(transitionTime);
            case CANCELLED -> purchaseOrder.setCancelledAt(transitionTime);
            case DRAFT -> throw new IllegalStateException("DRAFT is only an initial state");
        }

        setStatus(purchaseOrder, targetStatus);
    }

    private static void setStatus(PurchaseOrder purchaseOrder, PurchaseOrderStatus status) {
        purchaseOrder.transitionStatus(status);
    }

    private static Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> buildTransitions() {
        EnumMap<PurchaseOrderStatus, Set<PurchaseOrderStatus>> transitions =
                new EnumMap<>(PurchaseOrderStatus.class);

        transitions.put(PurchaseOrderStatus.DRAFT,
                EnumSet.of(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.CANCELLED));
        transitions.put(PurchaseOrderStatus.ISSUED,
                EnumSet.of(PurchaseOrderStatus.PARTIALLY_RECEIVED,
                        PurchaseOrderStatus.COMPLETED,
                        PurchaseOrderStatus.CANCELLED));
        transitions.put(PurchaseOrderStatus.PARTIALLY_RECEIVED,
                EnumSet.of(PurchaseOrderStatus.COMPLETED));
        transitions.put(PurchaseOrderStatus.COMPLETED,
                EnumSet.of(PurchaseOrderStatus.CLOSED));
        transitions.put(PurchaseOrderStatus.CLOSED,
                EnumSet.noneOf(PurchaseOrderStatus.class));
        transitions.put(PurchaseOrderStatus.CANCELLED,
                EnumSet.noneOf(PurchaseOrderStatus.class));

        return Map.copyOf(transitions);
    }
}
