package com.recnaile.purchaseorder.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo-backed sequence state used to allocate business-safe PO numbers.
 * One document is maintained per financial/calendar year represented in the PO number.
 */
@Document(collection = "purchase_order_sequences")
public class PurchaseOrderSequence {

    @Id
    private String id;

    private long sequence;

    public PurchaseOrderSequence() {
    }

    public PurchaseOrderSequence(String id, long sequence) {
        this.id = id;
        this.sequence = sequence;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
}
