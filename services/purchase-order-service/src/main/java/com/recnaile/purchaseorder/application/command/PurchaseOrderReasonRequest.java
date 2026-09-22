package com.recnaile.purchaseorder.application.command;

import jakarta.validation.constraints.NotBlank;

public record PurchaseOrderReasonRequest(
        @NotBlank String reason) {
}
