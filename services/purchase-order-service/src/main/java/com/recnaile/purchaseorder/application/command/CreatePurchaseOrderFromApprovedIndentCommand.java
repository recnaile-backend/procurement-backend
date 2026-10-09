package com.recnaile.purchaseorder.application.command;

import com.recnaile.purchaseorder.domain.enums.PurchaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Creates a PO from an approved READY_FOR_PO indent.
 * Indent-owned fields are read from Indent Service; Procurement supplies
 * only the PO-owned commercial fields allowed at creation time.
 */
public record CreatePurchaseOrderFromApprovedIndentCommand(
        @NotBlank String indentNumber,
        @NotNull PurchaseType purchaseType,
        @NotBlank String vendorName,
        LocalDate expectedDeliveryDate) {
}
