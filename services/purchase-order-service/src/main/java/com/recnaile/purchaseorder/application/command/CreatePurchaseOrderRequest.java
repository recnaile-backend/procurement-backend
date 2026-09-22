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
 * Manual/direct PO creation request. Use only when the procurement process
 * permits a PO to be raised without converting an approved indent.
 */
public record CreatePurchaseOrderRequest(
        @NotBlank String vendorName,
        String projectId,
        @NotBlank String departmentName,
        @NotNull PurchaseType purchaseType,
        LocalDate expectedDeliveryDate,
        @NotEmpty List<@Valid Item> items) {

    public record Item(
            @NotBlank String itemId,
            @NotBlank String itemName,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice,
            @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal gstRate) {
    }
}
