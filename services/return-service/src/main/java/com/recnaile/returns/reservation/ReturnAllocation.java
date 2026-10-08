package com.recnaile.returns.reservation;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** Concurrency guard for cumulative returns for one issued outward/item pair. */
@Document(collection = "return_allocations")
@CompoundIndex(name = "return_allocation_outward_item_unique", def = "{'outwardId': 1, 'itemId': 1}", unique = true)
public class ReturnAllocation {
    @Id private String id;
    private String outwardId;
    private String itemId;
    private Integer issuedQuantity;
    private Integer reservedQuantity;
    @Version private Long version;

    protected ReturnAllocation() {}
    public ReturnAllocation(String outwardId, String itemId, Integer issuedQuantity, Integer reservedQuantity) {
        this.outwardId = outwardId; this.itemId = itemId; this.issuedQuantity = issuedQuantity; this.reservedQuantity = reservedQuantity;
    }
    public String getId() { return id; }
    public String getOutwardId() { return outwardId; }
    public String getItemId() { return itemId; }
    public Integer getIssuedQuantity() { return issuedQuantity; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public Long getVersion() { return version; }
    public void setId(String id) { this.id = id; }
    public void setIssuedQuantity(Integer value) { this.issuedQuantity = value; }
    public void setReservedQuantity(Integer value) { this.reservedQuantity = value; }
    public void setVersion(Long value) { this.version = value; }
    public void reserve(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("quantity must be greater than zero");
        int current = reservedQuantity == null ? 0 : reservedQuantity;
        int issued = issuedQuantity == null ? 0 : issuedQuantity;
        if (current + quantity > issued) throw new IllegalStateException("Return quantity exceeds the remaining issued quantity");
        reservedQuantity = current + quantity;
    }
}
