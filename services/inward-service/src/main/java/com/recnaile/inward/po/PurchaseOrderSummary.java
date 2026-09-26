package com.recnaile.inward.po;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PurchaseOrderSummary(
        @JsonAlias({"id", "poId"}) String id,
        String status,
        String vendorName,
        List<PurchaseOrderLineSummary> items
) {
}
