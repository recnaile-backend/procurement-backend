package com.recnaile.inward.service;

import com.recnaile.inward.exception.PurchaseOrderNotEligibleException;
import com.recnaile.inward.po.PurchaseOrderClient;
import com.recnaile.inward.po.PurchaseOrderSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PurchaseOrderEligibilityService {

    private static final String ISSUED = "ISSUED";

    private final PurchaseOrderClient purchaseOrderClient;

    public PurchaseOrderEligibilityService(PurchaseOrderClient purchaseOrderClient) {
        this.purchaseOrderClient = purchaseOrderClient;
    }

    /**
     * Revalidates PO eligibility against the authoritative Purchase Order Service.
     *
     * No PO database is accessed by inward-service.
     */
    public PurchaseOrderSummary requireIssuedPurchaseOrder(String poId) {
        if (!StringUtils.hasText(poId)) {
            throw new IllegalArgumentException("poId must not be blank");
        }

        PurchaseOrderSummary purchaseOrder =
                purchaseOrderClient.getPurchaseOrder(poId);

        if (!ISSUED.equalsIgnoreCase(purchaseOrder.status())) {
            throw new PurchaseOrderNotEligibleException(
                    poId,
                    purchaseOrder.status()
            );
        }

        return purchaseOrder;
    }

    public boolean isEligible(String poId) {
        try {
            requireIssuedPurchaseOrder(poId);
            return true;
        } catch (PurchaseOrderNotEligibleException ex) {
            return false;
        }
    }
}
