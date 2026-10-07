package com.recnaile.inward.service;

import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.exception.GRNNotFoundException;
import com.recnaile.inward.exception.InvalidGRNStateTransitionException;
import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.model.GRNStatus;
import com.recnaile.inward.outbox.OutboxService;
import com.recnaile.inward.po.PurchaseOrderSummary;
import com.recnaile.inward.repository.GRNRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/** Confirms a draft GRN and publishes the goods-received event only after confirmation. */
@Service
public class GRNReceiptService {

    private final GRNRepository grnRepository;
    private final GRNStateMachine stateMachine;
    private final PurchaseOrderEligibilityService purchaseOrderEligibilityService;
    private final GRNReceiptProgressService receiptProgressService;
    private final OutboxService outboxService;
    private final Clock clock;
    private final IdempotencyService idempotencyService;

    public GRNReceiptService(
            GRNRepository grnRepository,
            GRNStateMachine stateMachine,
            PurchaseOrderEligibilityService purchaseOrderEligibilityService,
            GRNReceiptProgressService receiptProgressService,
            OutboxService outboxService,
            Clock clock,
            IdempotencyService idempotencyService) {
        this.grnRepository = grnRepository;
        this.stateMachine = stateMachine;
        this.purchaseOrderEligibilityService = purchaseOrderEligibilityService;
        this.receiptProgressService = receiptProgressService;
        this.outboxService = outboxService;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public GRNResponse receive(String grnId, String idempotencyKey, String principal) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return receiveInternal(grnId);
        }

        var record = idempotencyService.reserveAction(
                idempotencyKey, principal, "POST:/api/v1/goods-receipts/{grnId}/receive", "RECEIVE|" + grnId);
        if (record.getStatus() == com.recnaile.inward.model.IdempotencyRecord.Status.COMPLETED) {
            return grnRepository.findByGrnId(record.getGrnId())
                    .map(GRNResponse::from)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency record references missing GRN " + record.getGrnId()));
        }

        try {
            GRNResponse response = receiveInternal(grnId);
            idempotencyService.complete(record, response.grnId());
            return response;
        } catch (RuntimeException ex) {
            idempotencyService.release(record);
            throw ex;
        }
    }

    private GRNResponse receiveInternal(String grnId) {
        GRNDocument grn = grnRepository.findByGrnId(grnId)
                .orElseThrow(() -> new GRNNotFoundException(grnId));

        if (grn.getGrnStatus() != GRNStatus.DRAFT) {
            throw new InvalidGRNStateTransitionException(grn.getGrnStatus(), GRNStatus.RECEIVED);
        }

        PurchaseOrderSummary purchaseOrder =
                purchaseOrderEligibilityService.requireIssuedPurchaseOrder(grn.getPoId());

        // Calculate receipt progress while this GRN is still DRAFT. The repository
        // query used by the progress service may return the same in-memory entity
        // instance in tests (and can do so with some persistence implementations).
        // If we changed the status to RECEIVED first, the current GRN would be
        // counted by cumulativeReceivedByItem and then added again below, turning a
        // partial receipt into a false COMPLETED result.
        //
        // The current GRN is persisted as DRAFT, so cumulative progress excludes it.
        // Add this GRN's quantities explicitly for the final state.
        Map<String, BigDecimal> receivedBeforeThisGrn =
                receiptProgressService.cumulativeReceivedByItem(grn.getPoId());
        boolean fullyReceived = true;
        for (var line : grn.getLines() == null ? java.util.List.<com.recnaile.inward.model.GRNLine>of() : grn.getLines()) {
            BigDecimal before = receivedBeforeThisGrn.getOrDefault(line.itemName(), BigDecimal.ZERO);
            BigDecimal current = safe(line.acceptedQuantity()).add(safe(line.rejectedQuantity()));
            receivedBeforeThisGrn.put(line.itemName(), before.add(current));
        }
        for (var poItem : purchaseOrder.items() == null ? java.util.List.<com.recnaile.inward.po.PurchaseOrderLineSummary>of() : purchaseOrder.items()) {
            if (poItem == null || poItem.itemName() == null || poItem.quantity() == null) continue;
            if (receivedBeforeThisGrn.getOrDefault(poItem.itemName(), BigDecimal.ZERO)
                    .compareTo(poItem.quantity()) < 0) {
                fullyReceived = false;
                break;
            }
        }

        // Transition through RECEIVED so the domain state machine remains explicit,
        // then move to the calculated final receipt state.
        stateMachine.validateTransition(grn.getGrnStatus(), GRNStatus.RECEIVED);
        grn.setGrnStatus(GRNStatus.RECEIVED);

        GRNStatus finalStatus = fullyReceived ? GRNStatus.COMPLETED : GRNStatus.PARTIALLY_RECEIVED;
        stateMachine.validateTransition(grn.getGrnStatus(), finalStatus);
        grn.setGrnStatus(finalStatus);
        grn.setReceivedAt(Instant.now(clock));

        GRNDocument saved;
        try {
            saved = grnRepository.save(grn);
        } catch (OptimisticLockingFailureException ex) {
            throw ex;
        }

        GRNResponse response = GRNResponse.from(saved);
        outboxService.enqueueGoodsReceived(response);
        outboxService.enqueueGRNReceiptAuditEvent(response);
        return response;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
