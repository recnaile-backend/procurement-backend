package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.domain.model.PurchaseOrderLine;
import com.recnaile.purchaseorder.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Single source of truth for PO commercial calculations.
 * Client/event supplied totals are never trusted; all monetary values are
 * derived from quantity, unit price and GST rate on the server.
 */
@Component
public class PurchaseOrderCommercialCalculator {

    public static final int MONEY_SCALE = 2;
    private static final int GST_CALCULATION_SCALE = 8;

    public void calculate(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            throw new BusinessRuleException("PO_REQUIRED", "Purchase order is required for calculation");
        }
        List<PurchaseOrderLine> lines = purchaseOrder.getLines();
        if (lines == null || lines.isEmpty()) {
            throw new BusinessRuleException("PO_ITEMS_REQUIRED", "At least one PO line is required");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal gstAmount = BigDecimal.ZERO;

        for (PurchaseOrderLine line : lines) {
            calculateLine(line);
            subtotal = subtotal.add(line.getLineSubtotal());
            gstAmount = gstAmount.add(line.getGstAmount());
        }

        purchaseOrder.setSubtotal(money(subtotal));
        purchaseOrder.setGstAmount(money(gstAmount));
        purchaseOrder.setTotalAmount(money(subtotal.add(gstAmount)));
    }

    public void calculateLine(PurchaseOrderLine line) {
        if (line == null) {
            throw new BusinessRuleException("PO_LINE_REQUIRED", "Purchase order line is required");
        }

        BigDecimal quantity = requirePositive(line.getOrderedQuantity(), "quantity", line.getItemId());
        BigDecimal unitPrice = requireNonNegative(line.getUnitPrice(), "unitPrice", line.getItemId());
        BigDecimal gstRate = requireNonNegative(line.getGstRate(), "gstRate", line.getItemId());

        BigDecimal lineSubtotal = money(quantity.multiply(unitPrice));
        BigDecimal lineGst = money(lineSubtotal.multiply(gstRate)
                .divide(BigDecimal.valueOf(100), GST_CALCULATION_SCALE, RoundingMode.HALF_UP));
        BigDecimal lineTotal = money(lineSubtotal.add(lineGst));

        line.setUnitPrice(money(unitPrice));
        line.setGstRate(gstRate);
        line.setReceivedQuantity(nonNegativeOrZero(line.getReceivedQuantity()));
        line.setRemainingQuantity(quantity.subtract(line.getReceivedQuantity()));
        if (line.getRemainingQuantity().signum() < 0) {
            throw new BusinessRuleException("RECEIVED_QUANTITY_EXCEEDS_ORDERED",
                    "Received quantity cannot exceed ordered quantity for item: " + line.getItemId());
        }
        line.setLineSubtotal(lineSubtotal);
        line.setGstAmount(lineGst);
        line.setLineTotal(lineTotal);
    }

    private BigDecimal requirePositive(BigDecimal value, String field, String itemId) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleException("INVALID_" + field.toUpperCase(),
                    field + " must be greater than zero for item: " + itemId);
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String field, String itemId) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException("INVALID_" + field.toUpperCase(),
                    field + " cannot be negative for item: " + itemId);
        }
        return value;
    }

    private BigDecimal nonNegativeOrZero(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.signum() < 0) {
            throw new BusinessRuleException("INVALID_RECEIVED_QUANTITY", "Received quantity cannot be negative");
        }
        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
