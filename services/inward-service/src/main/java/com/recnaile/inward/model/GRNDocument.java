package com.recnaile.inward.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;

@Document(collection = "grns")
public class GRNDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String grnId;

    private String poId;
    @Field("vendorName")
    private String vendorName;

    // Legacy MongoDB field retained only for backward-compatible reads.
    @Field("supplierId")
    private String legacySupplierId;
    private String deliveryNoteNumber;
    private Instant deliveryDate;
    private Instant receivedAt;
    private String receivedBy;
    @Field("grnStatus")
    private GRNStatus grnStatus;

    // Legacy MongoDB field retained only for backward-compatible reads.
    @Field("status")
    private GRNStatus legacyStatus;
    private List<GRNLine> lines;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected GRNDocument() {
    }

    public GRNDocument(
            String id,
            String grnId,
            String poId,
            String vendorName,
            String deliveryNoteNumber,
            Instant deliveryDate,
            Instant receivedAt,
            String receivedBy,
            GRNStatus status,
            List<GRNLine> lines
    ) {
        this.id = id;
        this.grnId = grnId;
        this.poId = poId;
        this.vendorName = vendorName;
        this.deliveryNoteNumber = deliveryNoteNumber;
        this.deliveryDate = deliveryDate;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        this.grnStatus = status;
        this.lines = lines;
    }

    public String getId() {
        return id;
    }

    public String getGrnId() {
        return grnId;
    }

    public String getPoId() {
        return poId;
    }

    public String getVendorName() {
        return vendorName != null ? vendorName : legacySupplierId;
    }

    public String getDeliveryNoteNumber() {
        return deliveryNoteNumber;
    }

    public Instant getDeliveryDate() {
        return deliveryDate;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public GRNStatus getGrnStatus() {
        return grnStatus != null ? grnStatus : legacyStatus;
    }

    public List<GRNLine> getLines() {
        return lines;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
