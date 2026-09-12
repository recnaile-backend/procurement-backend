package com.recnaile.masterdata.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "items")
public class Item {

    /** Stable Item ID / SKU used by downstream services. */
    @Id
    private String itemId;

    private String name;

    private String category;

    private String unitOfMeasure;

    private ItemType type;

    /** Department to which this item belongs. */
    private String department;

    /** Whether this item is tracked as an asset when issued. */
    private boolean assetTracked;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal defaultUnitPrice;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal gstRate;

    /** Barcode/QR-ready identifier; null when not configured. */
    @Indexed(sparse = true)
    private String barcodeQrIdentifier;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal minimumStock;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maximumStock;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal availableStock;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal defectStock;

    /** Physical rack/location of the item. */
    private String rack;

    /** Additional item remarks. */
    private String remark;

    private MasterDataStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;
}