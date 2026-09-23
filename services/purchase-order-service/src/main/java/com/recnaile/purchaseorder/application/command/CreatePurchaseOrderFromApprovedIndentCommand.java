package com.recnaile.purchaseorder.application.command;

import com.recnaile.purchaseorder.domain.enums.PurchaseType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Converts an approved READY_FOR_PO indent into a Purchase Order.
 * The indent status is never supplied by the client; it is read from Indent Service.
 */
public record CreatePurchaseOrderFromApprovedIndentCommand(
        @NotBlank String indentNumber,
        @NotBlank String vendorName,
        String projectId,
        @NotBlank String departmentName,
        @NotNull PurchaseType purchaseType,
        LocalDate expectedDeliveryDate,
        @NotEmpty List<@Valid ApprovedIndentLine> items) {

    public record ApprovedIndentLine(
            @NotBlank String itemId,
            @NotBlank String itemName,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal gstRate) {
    }
}
