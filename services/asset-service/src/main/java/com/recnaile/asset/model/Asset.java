package com.recnaile.asset.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Aggregate root for a reusable/asset-tracked item.
 *
 * <p>The aggregate stores the current asset identity and lifecycle snapshot.
 * Stock quantities and stock movements are deliberately outside this model.
 * Historical assignment records are introduced in the assignment-history phase.</p>
 */
@Document(collection = "assets")
public class Asset {

    @Id
    private String assetId;

    @Indexed
    private String itemId;

    /** Approved/issued outward reference that created this asset lifecycle. */
    @Indexed(unique = true)
    private String issueReference;

    private String name;
    private String brand;
    private String model;

    @Indexed
    private String departmentName;

    @Indexed
    private String assignedUserId;

    private String issuedBy;

    @Indexed
    private AssetStatus status;

    private AssetCondition condition;
    private String remarks;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    protected Asset() {
        // Required by Spring Data MongoDB.
    }

    private Asset(
            String assetId,
            String itemId,
            String issueReference,
            String name,
            String brand,
            String model,
            String departmentName,
            String assignedUserId,
            String issuedBy,
            AssetStatus status,
            AssetCondition condition,
            String remarks,
            Instant createdAt,
            Instant updatedAt) {
        this.assetId = assetId;
        this.itemId = itemId;
        this.issueReference = issueReference;
        this.name = name;
        this.brand = brand;
        this.model = model;
        this.departmentName = departmentName;
        this.assignedUserId = assignedUserId;
        this.issuedBy = issuedBy;
        this.status = status;
        this.condition = condition;
        this.remarks = remarks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates an asset lifecycle snapshot. Assignment is intentionally a
     * separate concern and is not created by the aggregate factory.
     */
    public static Asset create(
            AssetId assetId,
            String itemId,
            String issueReference,
            String name,
            String brand,
            String model,
            String issuedBy,
            AssetStatus status,
            AssetCondition condition,
            String remarks,
            Instant now) {
        if (assetId == null) {
            throw new IllegalArgumentException("Asset ID is required");
        }
        requireText(itemId, "Item ID");
        requireText(issueReference, "Issue reference");
        requireText(name, "Asset name");
        requireText(issuedBy, "Issuer ID");
        if (status == null) {
            throw new IllegalArgumentException("Asset status is required");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Asset condition is required");
        }
        if (now == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }

        return new Asset(
                assetId.value(),
                itemId,
                issueReference,
                name,
                brand,
                model,
                null,
                null,
                issuedBy,
                status,
                condition,
                remarks,
                now,
                now);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }


    /** Assigns the asset for a new lifecycle. A retired or repair-state asset cannot be assigned. */
    public void assignTo(String assignedUserId, String departmentName, Instant now) {
        requireText(assignedUserId, "Assigned user ID");
        requireText(departmentName, "Department name");
        if (now == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        if (this.assignedUserId != null) {
            throw new IllegalStateException("Asset already has an assigned user");
        }
        if (this.status == AssetStatus.RETIRED) {
            throw new IllegalStateException("Retired asset cannot be assigned");
        }
        if (this.status == AssetStatus.UNDER_REPAIR) {
            throw new IllegalStateException("Asset under repair cannot be assigned");
        }
        if (this.status != AssetStatus.RETURNED) {
            throw new IllegalStateException("Asset must be in RETURNED state before assignment");
        }
        this.assignedUserId = assignedUserId;
        this.departmentName = departmentName;
        this.status = AssetStatus.IN_USE;
        this.updatedAt = now;
    }

    /** Marks an issued asset as returned and closes its current lifecycle state. */
    public void markReturned(AssetCondition returnedCondition, Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        if (this.status != AssetStatus.IN_USE) {
            throw new IllegalStateException("Only an IN_USE asset can be returned");
        }
        if (returnedCondition == null) {
            throw new IllegalArgumentException("Return condition is required");
        }
        this.assignedUserId = null;
        if (returnedCondition == AssetCondition.DAMAGED || returnedCondition == AssetCondition.UNDER_REPAIR) {
            this.condition = returnedCondition;
            this.status = AssetStatus.UNDER_REPAIR;
        } else {
            this.condition = AssetCondition.GOOD;
            this.status = AssetStatus.RETURNED;
        }
        this.updatedAt = now;
    }

    /** Completes repair and makes the asset available for a new lifecycle. */
    public void completeRepair(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        if (this.status != AssetStatus.UNDER_REPAIR) {
            throw new IllegalStateException("Only an UNDER_REPAIR asset can complete repair");
        }
        this.condition = AssetCondition.GOOD;
        this.status = AssetStatus.RETURNED;
        this.updatedAt = now;
    }

    /** Retires an available asset. Retired is a terminal lifecycle state. */
    public void retire(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        if (this.status == AssetStatus.RETIRED) {
            throw new IllegalStateException("Asset is already retired");
        }
        if (this.status == AssetStatus.IN_USE) {
            throw new IllegalStateException("IN_USE asset must be returned before retirement");
        }
        if (this.assignedUserId != null) {
            throw new IllegalStateException("Assigned asset cannot be retired");
        }
        this.status = AssetStatus.RETIRED;
        this.updatedAt = now;
    }

    /** Clears the current assignment snapshot while preserving assignment history elsewhere. */
    public void clearAssignment(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
        this.assignedUserId = null;
        this.updatedAt = now;
    }

    public String getAssetId() { return assetId; }
    public String getItemId() { return itemId; }
    public String getIssueReference() { return issueReference; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getDepartmentName() { return departmentName; }
    public String getAssignedUserId() { return assignedUserId; }
    public String getIssuedBy() { return issuedBy; }
    public AssetStatus getStatus() { return status; }
    public AssetCondition getCondition() { return condition; }
    public String getRemarks() { return remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
