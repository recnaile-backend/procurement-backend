package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.IdempotencyRecord;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.state.PurchaseOrderStateMachine;
import com.recnaile.purchaseorder.exception.ConcurrentPurchaseOrderModificationException;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class PurchaseOrderLifecycleService {

    private final Clock clock;
    private final PurchaseOrderRepository repository;
    private final PurchaseOrderQueryService queryService;
    private final PurchaseOrderIdempotencyService idempotencyService;

    public PurchaseOrderLifecycleService(PurchaseOrderRepository repository,
                                         PurchaseOrderQueryService queryService,
                                         PurchaseOrderIdempotencyService idempotencyService) {
        this(repository, queryService, idempotencyService, Clock.systemUTC());
    }

    PurchaseOrderLifecycleService(PurchaseOrderRepository repository,
                                   PurchaseOrderQueryService queryService,
                                   PurchaseOrderIdempotencyService idempotencyService,
                                   Clock clock) {
        this.repository = repository;
        this.queryService = queryService;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    public PurchaseOrder transitionAndSave(String poId,
                                           PurchaseOrderStatus targetStatus,
                                           String actorId,
                                           String idempotencyKey) {

        String operation = "TRANSITION_" + targetStatus.name();
        String requestHash = idempotencyService.requestHash(
                operation,
                poId,
                targetStatus.name());

        PurchaseOrderIdempotencyService.Reservation reservation =
                idempotencyService.reserve(actorId, idempotencyKey, operation, requestHash);

        IdempotencyRecord record = reservation.record();

        if (!reservation.newlyReserved()) {
            idempotencyService.assertSameRequest(record, operation, requestHash);
            return replay(record);
        }

        try {
            PurchaseOrder po = queryService.getById(poId);
            PurchaseOrderStateMachine.transition(po, targetStatus, Instant.now(clock));
            po.setUpdatedBy(actorId);

            PurchaseOrder saved = repository.save(po);
            idempotencyService.complete(record, saved.getPoId(), 200);
            return saved;
        } catch (OptimisticLockingFailureException ex) {
            idempotencyService.release(record);
            throw new ConcurrentPurchaseOrderModificationException(
                    "Purchase Order was modified by another request. Refresh and retry the operation.");
        } catch (RuntimeException ex) {
            idempotencyService.release(record);
            throw ex;
        }
    }

    private PurchaseOrder replay(IdempotencyRecord record) {
        if (record.getResourceId() == null || record.getResourceId().isBlank()) {
            throw new com.recnaile.purchaseorder.exception.BusinessRuleException(
                    "IDEMPOTENCY_RESOURCE_NOT_FOUND",
                    "The previous idempotent operation has no stored purchase order reference");
        }
        return queryService.getById(record.getResourceId());
    }
}
