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

    public OutwardItem() {
    }

    public OutwardItem(String itemId, BigDecimal quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
