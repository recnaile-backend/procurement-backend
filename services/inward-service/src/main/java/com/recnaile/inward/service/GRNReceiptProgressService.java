package com.recnaile.inward.service;

import com.recnaile.inward.exception.ReceivedQuantityExceedsOrderedException;
import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.model.GRNLine;
import com.recnaile.inward.po.PurchaseOrderLineSummary;
import com.recnaile.inward.po.PurchaseOrderSummary;
import com.recnaile.inward.repository.GRNRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks cumulative receipt quantities for a PO without accessing the PO database.
 * Accepted and rejected quantities both consume the ordered quantity because both
 * represent physical quantity received against the PO line.
 */
@Service
public class GRNReceiptProgressService {

    private final GRNRepository grnRepository;

    public GRNReceiptProgressService(GRNRepository grnRepository) {
        this.grnRepository = grnRepository;
    }

    public void validateRequestedQuantities(
            String poId,
            PurchaseOrderSummary purchaseOrder,
            List<GRNLine> requestedLines
    ) {
        Map<String, BigDecimal> previouslyReceived = cumulativeReceivedByItem(poId);
        List<PurchaseOrderLineSummary> poItems = purchaseOrder.items() == null ? List.of() : purchaseOrder.items();
        for (GRNLine line : requestedLines) {
            BigDecimal requested = safe(line.acceptedQuantity()).add(safe(line.rejectedQuantity()));
            BigDecimal prior = previouslyReceived.getOrDefault(line.itemName(), BigDecimal.ZERO);
            BigDecimal cumulative = prior.add(requested);

            PurchaseOrderLineSummary poLine = poItems.stream()
                    .filter(item -> item != null && line.itemName().equals(item.itemName()))
                    .findFirst()
                    .orElse(null);

            if (poLine == null) {
                continue;
            }

            if (cumulative.compareTo(poLine.quantity()) > 0) {
                throw new ReceivedQuantityExceedsOrderedException(
                        poId,
                        line.itemName(),
                        poLine.quantity(),
                        prior,
                        requested
                );
            }

        }
    }

    /**
     * Returns cumulative confirmed receipt quantity per item across all GRNs for the PO.
     * DRAFT GRNs are intentionally excluded because creation and receipt confirmation
     * are separate business actions.
     */
    public Map<String, BigDecimal> cumulativeReceivedByItem(String poId) {
        Map<String, BigDecimal> totals = new HashMap<>();

        for (GRNDocument grn : grnRepository.findByPoId(poId, org.springframework.data.domain.Sort.unsorted())) {
            // DRAFT GRNs are not confirmed receipts and must not consume PO quantity.
            if (grn.getGrnStatus() == null || grn.getGrnStatus() == com.recnaile.inward.model.GRNStatus.DRAFT) {
                continue;
            }
            if (grn.getLines() == null) {
                continue;
            }
            for (GRNLine line : grn.getLines()) {
                BigDecimal quantity = safe(line.acceptedQuantity()).add(safe(line.rejectedQuantity()));
                totals.merge(line.itemName(), quantity, BigDecimal::add);
            }
        }

        return totals;
    }

    public GRNReceiptProgress calculateProgress(
            String poId,
            PurchaseOrderSummary purchaseOrder
    ) {
        Map<String, BigDecimal> received = cumulativeReceivedByItem(poId);
        Map<String, BigDecimal> remaining = new HashMap<>();
        boolean fullyReceived = true;

        for (PurchaseOrderLineSummary item : purchaseOrder.items() == null ? List.<PurchaseOrderLineSummary>of() : purchaseOrder.items()) {
            if (item == null || item.itemName() == null || item.quantity() == null) {
                continue;
            }

            BigDecimal receivedQuantity = safe(received.get(item.itemName()));
            BigDecimal remainingQuantity = item.quantity().subtract(receivedQuantity).max(BigDecimal.ZERO);
            remaining.put(item.itemName(), remainingQuantity);

            if (remainingQuantity.signum() > 0) {
                fullyReceived = false;
            }
        }

        return new GRNReceiptProgress(poId, received, remaining, fullyReceived);
    }

    public boolean isPurchaseOrderFullyReceived(
            String poId,
            PurchaseOrderSummary purchaseOrder
    ) {
        return calculateProgress(poId, purchaseOrder).fullyReceived();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record GRNReceiptProgress(
            String poId,
            Map<String, BigDecimal> receivedQuantities,
            Map<String, BigDecimal> remainingQuantities,
            boolean fullyReceived
    ) {
    }
}
