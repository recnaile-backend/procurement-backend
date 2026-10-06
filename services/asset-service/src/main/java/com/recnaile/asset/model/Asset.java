package com.recnaile.asset.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Aggregate root for a reusable/asset-tracked item.
 *
 * <p>The aggregate stores the current assignment snapshot. Assignment history
 * is stored separately in {@link AssetAssignment}; historical records are
 * never deleted by this service.</p>
 */
@Document(collection = "assets")
public class Asset {

    @Id
    private String assetId;

    @Indexed(unique = true)
    private String assetReference;

    @Indexed
    private String itemId;

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
            String assetReference,
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
        this.assetReference = assetReference;
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

    /** Starts the current assignment snapshot for this asset. */
    public void assignTo(String employeeId, String departmentName, Instant now) {
        requireText(employeeId, "Employee ID");
        requireText(departmentName, "Department name");
        requireTimestamp(now);
        if (assignedUserId != null) {
            throw new IllegalStateException("Asset already has an active assignment");
        }
        if (status != AssetStatus.IN_USE) {
            throw new IllegalStateException("Only an IN_USE asset can be assigned");
        }
        this.assignedUserId = employeeId;
        this.departmentName = departmentName;
        this.updatedAt = now;
    }

    /**
     * Applies a return outcome to the asset lifecycle. A good return makes the
     * asset available for another lifecycle; a damaged return moves it to repair.
     */
    public void returnFromAssignment(AssetCondition returnCondition, Instant now) {
        requireTimestamp(now);
        if (returnCondition == null) {
            throw new IllegalArgumentException("Return condition is required");
        }
        if (status != AssetStatus.IN_USE) {
            throw new IllegalStateException("Only an IN_USE asset can be returned");
        }
        if (assignedUserId == null) {
            throw new IllegalStateException("Asset has no active assignment");
        }

        this.condition = returnCondition;
        this.status = returnCondition == AssetCondition.GOOD
                ? AssetStatus.RETURNED
                : AssetStatus.UNDER_REPAIR;
        clearAssignment(now);
    }

    /** Starts a new lifecycle after a good return. */
    public void beginNewLifecycle(String employeeId, String departmentName, Instant now) {
        if (status != AssetStatus.RETURNED) {
            throw new IllegalStateException("Only a RETURNED asset can start a new lifecycle");
        }
        this.status = AssetStatus.IN_USE;
        assignTo(employeeId, departmentName, now);
    }

    /** Marks a repaired asset as available for its next lifecycle. */
    public void completeRepair(Instant now) {
        requireTimestamp(now);
        if (status != AssetStatus.UNDER_REPAIR) {
            throw new IllegalStateException("Only an UNDER_REPAIR asset can complete repair");
        }
        this.condition = AssetCondition.GOOD;
        this.status = AssetStatus.RETURNED;
        this.updatedAt = now;
    }

    /** Retired is terminal and cannot be assigned again. */
    public void retire(Instant now) {
        requireTimestamp(now);
        if (assignedUserId != null) {
            throw new IllegalStateException("Assigned asset cannot be retired");
        }
        if (status == AssetStatus.RETIRED) {
            throw new IllegalStateException("Asset is already retired");
        }
        if (status != AssetStatus.RETURNED) {
            throw new IllegalStateException("Only a RETURNED asset can be retired");
        }
        this.status = AssetStatus.RETIRED;
        this.updatedAt = now;
    }

    /** Clears the current assignment snapshot after the assignment is closed. */
    public void clearAssignment(Instant now) {
        requireTimestamp(now);
        this.assignedUserId = null;
        this.departmentName = null;
        this.updatedAt = now;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void requireTimestamp(Instant value) {
        if (value == null) {
            throw new IllegalArgumentException("Timestamp is required");
        }
    }

    public String getAssetId() { return assetId; }
    public String getAssetReference() { return assetReference; }
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
