package com.recnaile.outward.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class OutwardItem {

    @NotBlank
    private String itemId;

    @NotNull
    @Positive
    private BigDecimal quantity;

    /** Derived from Master Data; never accepted from the client. */
    private boolean assetTracked;

    public OutwardItem() {
    }

    public OutwardItem(String itemId, BigDecimal quantity) {
        this(itemId, quantity, false);
    }

    public OutwardItem(String itemId, BigDecimal quantity, boolean assetTracked) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.assetTracked = assetTracked;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public boolean isAssetTracked() { return assetTracked; }
    public void setAssetTracked(boolean assetTracked) { this.assetTracked = assetTracked; }
}
