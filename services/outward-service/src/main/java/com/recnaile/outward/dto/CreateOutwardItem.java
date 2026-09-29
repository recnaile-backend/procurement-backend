package com.recnaile.outward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Schema(description = "Item and quantity requested for outward issue.")
public class CreateOutwardItem {
    @Schema(description = "Master Data item identifier", example = "ITEM-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank private String itemId;
    @Schema(description = "Requested quantity", example = "2", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0.000001")
    @NotNull @Positive private BigDecimal quantity;
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
