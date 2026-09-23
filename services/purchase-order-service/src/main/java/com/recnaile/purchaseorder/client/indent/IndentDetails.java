package com.recnaile.purchaseorder.client.indent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record IndentDetails(
        String indentNumber,
        String status,
        String vendorName,
        String projectId,
        String departmentName,
        LocalDate expectedDeliveryDate,
        List<Item> items) {

    public record Item(
            String itemId,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal gstRate) {
    }
}
