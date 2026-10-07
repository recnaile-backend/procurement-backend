package com.recnaile.inward.service;

import com.recnaile.inward.dto.CreateGRNLineRequest;
import com.recnaile.inward.dto.CreateGRNRequest;
import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.model.GRNStatus;
import com.recnaile.inward.exception.ItemNotInPurchaseOrderException;
import com.recnaile.inward.exception.DuplicatePurchaseOrderItemException;
import com.recnaile.inward.exception.RejectionReasonRequiredException;
import com.recnaile.inward.exception.DuplicateDeliveryNoteException;
import com.recnaile.inward.exception.DuplicateGRNItemException;
import com.recnaile.inward.po.PurchaseOrderLineSummary;
import com.recnaile.inward.po.PurchaseOrderSummary;
import com.recnaile.inward.repository.GRNRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.recnaile.inward.model.IdempotencyRecord;
import com.recnaile.inward.outbox.OutboxService;
import com.recnaile.inward.observability.InwardMetrics;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GRNCreationService {

    private final GRNRepository grnRepository;
    private final PurchaseOrderEligibilityService purchaseOrderEligibilityService;
    private final Clock clock;
    private final GRNNumberGenerator grnNumberGenerator;
    private final GRNReceiptProgressService receiptProgressService;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final InwardMetrics metrics;

    public GRNCreationService(
            GRNRepository grnRepository,
            PurchaseOrderEligibilityService purchaseOrderEligibilityService,
            Clock clock,
            GRNNumberGenerator grnNumberGenerator,
            GRNReceiptProgressService receiptProgressService
    ) {
        this(grnRepository, purchaseOrderEligibilityService, clock, grnNumberGenerator, receiptProgressService, null, null, null);
    }

    @Autowired
    public GRNCreationService(
            GRNRepository grnRepository,
            PurchaseOrderEligibilityService purchaseOrderEligibilityService,
            Clock clock,
            GRNNumberGenerator grnNumberGenerator,
            GRNReceiptProgressService receiptProgressService,
            IdempotencyService idempotencyService,
            OutboxService outboxService,
            InwardMetrics metrics
    ) {
        this.grnRepository = grnRepository;
        this.purchaseOrderEligibilityService = purchaseOrderEligibilityService;
        this.clock = clock;
        this.grnNumberGenerator = grnNumberGenerator;
        this.receiptProgressService = receiptProgressService;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.metrics = metrics;
    }

    @Transactional
    public GRNResponse create(CreateGRNRequest request) {
        GRNResponse response = createInternal(request);
        if (outboxService != null) {
            outboxService.enqueueGRNCreateAuditEvent(response);
        }
        return response;
    }

    @Transactional
    public GRNResponse create(CreateGRNRequest request, String idempotencyKey, String principal) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyService == null) {
            GRNResponse response = createInternal(request);
            if (outboxService != null) {
                outboxService.enqueueGRNCreateAuditEvent(response);
            }
            return response;
        }

        IdempotencyRecord record = idempotencyService.reserve(idempotencyKey, principal, request);
        if (record.getStatus() == IdempotencyRecord.Status.COMPLETED) {
            return grnRepository.findByGrnId(record.getGrnId())
                    .map(GRNResponse::from)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency record references missing GRN " + record.getGrnId()));
        }

        try {
            GRNResponse response = createInternal(request);
            if (outboxService != null) {
                outboxService.enqueueGRNCreateAuditEvent(response);
            }
            idempotencyService.complete(record, response.grnId());
            return response;
        } catch (RuntimeException ex) {
            idempotencyService.release(record);
            throw ex;
        }
    }

    public GRNCreationService(
            GRNRepository grnRepository,
            PurchaseOrderEligibilityService purchaseOrderEligibilityService,
            Clock clock,
            GRNNumberGenerator grnNumberGenerator,
            GRNReceiptProgressService receiptProgressService,
            IdempotencyService idempotencyService,
            OutboxService outboxService) {
        this(grnRepository, purchaseOrderEligibilityService, clock, grnNumberGenerator,
                receiptProgressService, idempotencyService, outboxService, null);
    }

    private GRNResponse createInternal(CreateGRNRequest request) {
        PurchaseOrderSummary purchaseOrder =
                purchaseOrderEligibilityService.requireIssuedPurchaseOrder(request.poId());

        if (grnRepository.existsByPoIdAndDeliveryNoteNumber(
                request.poId(), request.deliveryNoteNumber())) {
            throw new DuplicateDeliveryNoteException(
                    request.poId(), request.deliveryNoteNumber());
        }

        Map<String, PurchaseOrderLineSummary> poItems = new HashMap<>();
        for (PurchaseOrderLineSummary item : purchaseOrder.items() == null ? List.<PurchaseOrderLineSummary>of() : purchaseOrder.items()) {
            if (item == null || item.itemName() == null || item.itemName().isBlank()) {
                continue;
            }

            if (poItems.putIfAbsent(item.itemName(), item) != null) {
                throw new DuplicatePurchaseOrderItemException(request.poId(), item.itemName());
            }
        }

        Set<String> requestItemNames = new HashSet<>();
        List<CreateGRNLineRequest> requestedItems = request.items() == null ? List.of() : request.items();
        List<GRNLine> grnLines = requestedItems.stream()
                .map(item -> {
                    if (!requestItemNames.add(item.itemName())) {
                        throw new DuplicateGRNItemException(item.itemName());
                    }
                    return toGRNLine(item, poItems, request.poId());
                })
                .toList();

        // Phase 06/07: validate this receipt against cumulative quantities from
        // all existing GRNs for the PO before persisting the new GRN.
        receiptProgressService.validateRequestedQuantities(
                request.poId(), purchaseOrder, grnLines);

        GRNDocument grn = new GRNDocument(
                null,
                grnNumberGenerator.next(),
                purchaseOrder.id(),
                purchaseOrder.vendorName(),
                request.deliveryNoteNumber(),
                request.deliveryDate(),
                null,
                request.receivedBy(),
                GRNStatus.DRAFT,
                grnLines
        );

        GRNResponse response = GRNResponse.from(grnRepository.save(grn));
        if (metrics != null) {
            metrics.grnCreated();
        }
        return response;
    }

    private GRNLine toGRNLine(
            CreateGRNLineRequest request,
            Map<String, PurchaseOrderLineSummary> poItems,
            String poId
    ) {
        PurchaseOrderLineSummary poItem = poItems.get(request.itemName());

        if (poItem == null) {
            throw new ItemNotInPurchaseOrderException(request.itemName(), poId);
        }

        if (request.rejectedQuantity().signum() > 0
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new RejectionReasonRequiredException();
        }

        return new GRNLine(
                poItem.itemName(),
                poItem.quantity(),
                request.acceptedQuantity(),
                request.rejectedQuantity(),
                request.unitOfMeasure(),
                request.rejectedQuantity().signum() == 0 ? null : request.rejectionReason()
        );
    }
}
