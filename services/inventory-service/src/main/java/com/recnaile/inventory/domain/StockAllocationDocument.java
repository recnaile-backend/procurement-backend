package com.recnaile.inventory.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Links an issued Outward item to the exact Inventory balance that was reduced.
 * This lets a later return restore stock to the same balance dimension without
 * requiring Return Service to know Inventory's internal balance key.
 */
@Document("stock_allocations")
@CompoundIndex(name = "uk_stock_allocation_outward_item", def = "{'outwardId':1,'itemId':1}", unique = true)
public class StockAllocationDocument {
    @Id
    private String id;
    @Indexed
    private String outwardId;
    @Indexed
    private String itemId;
    private String balanceKey;
    private int issuedQuantity;
    private int returnedQuantity;
    private Instant createdAt;
    private Instant updatedAt;
    @Version
    private Long version;

    public StockAllocationDocument() {}

    public StockAllocationDocument(String outwardId, String itemId, String balanceKey, int issuedQuantity, Instant now) {
        this.outwardId = outwardId;
        this.itemId = itemId;
        this.balanceKey = balanceKey;
        this.issuedQuantity = issuedQuantity;
        this.returnedQuantity = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getOutwardId() { return outwardId; }
    public String getItemId() { return itemId; }
    public String getBalanceKey() { return balanceKey; }
    public int getIssuedQuantity() { return issuedQuantity; }
    public int getReturnedQuantity() { return returnedQuantity; }
    public int getRemainingQuantity() { return issuedQuantity - returnedQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setId(String id) { this.id = id; }
    public void setOutwardId(String outwardId) { this.outwardId = outwardId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setBalanceKey(String balanceKey) { this.balanceKey = balanceKey; }
    public void setIssuedQuantity(int issuedQuantity) { this.issuedQuantity = issuedQuantity; }
    public void setReturnedQuantity(int returnedQuantity) { this.returnedQuantity = returnedQuantity; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }

    public void recordReturn(int quantity, Instant now) {
        if (quantity <= 0) throw new IllegalArgumentException("Return quantity must be greater than zero");
        if (returnedQuantity + quantity > issuedQuantity) {
            throw new IllegalArgumentException("Return quantity exceeds the remaining issued quantity");
        }
        returnedQuantity += quantity;
        updatedAt = now;
    }
}
