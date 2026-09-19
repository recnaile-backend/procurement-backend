package com.recnaile.purchaseorder.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderFromApprovedIndentCommand(
        @NotBlank String indentId,
        @NotBlank String vendorId,
        @NotBlank String indentStatus,
        String projectId,
        @NotBlank String departmentId,
        LocalDate expectedDeliveryDate,
        String remarks,
        @NotEmpty List<@Valid ApprovedIndentLine> items) {

    public record ApprovedIndentLine(
            @NotBlank String itemId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate) {
    }
}
