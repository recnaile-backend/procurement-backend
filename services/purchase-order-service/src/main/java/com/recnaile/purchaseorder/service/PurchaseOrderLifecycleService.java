package com.recnaile.purchaseorder.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.state.PurchaseOrderStateMachine;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.exception.ConcurrentPurchaseOrderModificationException;
import com.recnaile.purchaseorder.observability.PurchaseOrderMetrics;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;

import io.micrometer.observation.annotation.Observed;

@Service
public class PurchaseOrderLifecycleService {

    private final Clock clock;
    private final PurchaseOrderRepository repository;
    private final PurchaseOrderQueryService queryService;
    private final PurchaseOrderIdempotencyService idempotencyService;
    private final PurchaseOrderOutboxService outboxService;
    private final PurchaseOrderMetrics metrics;

    public PurchaseOrderLifecycleService(
            PurchaseOrderRepository repository,
            PurchaseOrderQueryService queryService,
            PurchaseOrderIdempotencyService idempotencyService,
            PurchaseOrderOutboxService outboxService,
            PurchaseOrderMetrics metrics) {
        this.repository = repository;
        this.queryService = queryService;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.metrics = metrics;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    @Observed(name = "purchase.order.command")
    public PurchaseOrder transitionAndSave(
            String poId,
            PurchaseOrderStatus targetStatus,
            String reason,
            String actorId,
            String idempotencyKey) {

        if ((targetStatus == PurchaseOrderStatus.ISSUED || targetStatus == PurchaseOrderStatus.CANCELLED)
                && (reason == null || reason.isBlank())) {
            throw new BusinessRuleException(
                    targetStatus == PurchaseOrderStatus.ISSUED ? "ISSUE_REASON_REQUIRED" : "CANCEL_REASON_REQUIRED",
                    targetStatus == PurchaseOrderStatus.ISSUED
                            ? "A reason is required when issuing a purchase order"
                            : "A reason is required when cancelling a purchase order");
        }

        String operation = "TRANSITION_" + targetStatus.name();
        String normalizedReason = reason == null ? null : reason.trim();
        String requestHash = null;
        PurchaseOrderIdempotencyService.Reservation reservation = null;
        IdempotencyRecord record = null;

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = idempotencyService.requestHash(
                    operation,
                    poId,
                    new TransitionFingerprint(targetStatus.name(), normalizedReason));
            reservation = idempotencyService.reserve(actorId, idempotencyKey, operation, requestHash);
            record = reservation.record();

            if (!reservation.newlyReserved()) {
                idempotencyService.assertSameRequest(record, operation, requestHash);
                return replay(record);
            }
        }

        try {
            PurchaseOrder po = queryService.getById(poId);
            PurchaseOrderStateMachine.transition(po, targetStatus, Instant.now(clock));

            if (targetStatus == PurchaseOrderStatus.ISSUED) {
                po.setIssueReason(normalizedReason);
            } else if (targetStatus == PurchaseOrderStatus.CANCELLED) {
                po.setCancellationReason(normalizedReason);
            }

            po.setUpdatedBy(actorId);
            PurchaseOrder saved = repository.save(po);
            publishLifecycleEvent(saved, targetStatus, actorId);
            if (record != null) {
                idempotencyService.complete(record, saved.getPoId(), 200);
            }

            if (metrics != null) {
                metrics.recordLifecycleTransition(targetStatus.name());
            }
            return saved;
        } catch (OptimisticLockingFailureException ex) {
            if (record != null) {
                idempotencyService.release(record);
            }
            throw new ConcurrentPurchaseOrderModificationException(
                    "Purchase Order was modified by another request. Refresh and retry the operation.");
        } catch (RuntimeException ex) {
            if (record != null) {
                idempotencyService.release(record);
            }
            throw ex;
        }
    }

    private record TransitionFingerprint(String targetStatus, String reason) {}

    private void publishLifecycleEvent(PurchaseOrder purchaseOrder,
                                       PurchaseOrderStatus targetStatus,
                                       String actorId) {
        switch (targetStatus) {
            case ISSUED -> outboxService.enqueueIssued(purchaseOrder, actorId);
            default -> { }
        }
    }

    private PurchaseOrder replay(IdempotencyRecord record) {
        if (record.getResourceId() == null || record.getResourceId().isBlank()) {
            throw new BusinessRuleException(
                    "IDEMPOTENCY_RESOURCE_NOT_FOUND",
                    "The previous idempotent operation has no stored purchase order reference");
        }
        return queryService.getById(record.getResourceId());
    }
}
