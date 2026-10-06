package com.recnaile.asset.assignment.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Immutable-in-purpose historical record for one asset assignment lifecycle.
 * Records are never deleted; closing an assignment only sets its end state.
 */
@Document(collection = "asset_assignments")
@CompoundIndex(name = "asset_active_assignment_idx", def = "{'assetId': 1, 'active': 1}")
@CompoundIndex(name = "employee_assignment_history_idx", def = "{'assignedUserId': 1, 'assignedAt': -1}")
public class AssetAssignment {

    @Id
    private String assignmentId;

    @Indexed
    private String assetId;

    @Indexed
    private String itemId;

    @Indexed
    private String assignedUserId;

    @Indexed
    private String departmentName;

    private String assignedBy;
    private Instant assignedAt;
    private Instant returnedAt;
    private boolean active;
    private String remarks;

    @Version
    private Long version;

    protected AssetAssignment() {
        // Required by Spring Data MongoDB.
    }

    private AssetAssignment(
            String assignmentId,
            String assetId,
            String itemId,
            String assignedUserId,
            String departmentName,
            String assignedBy,
            Instant assignedAt,
            String remarks) {
        this.assignmentId = assignmentId;
        this.assetId = assetId;
        this.itemId = itemId;
        this.assignedUserId = assignedUserId;
        this.departmentName = departmentName;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.remarks = remarks;
        this.active = true;
    }

    public static AssetAssignment create(
            String assetId,
            String itemId,
            String assignedUserId,
            String departmentName,
            String assignedBy,
            Instant assignedAt,
            String remarks) {
        requireText(assetId, "Asset ID");
        requireText(itemId, "Item ID");
        requireText(assignedUserId, "Assigned user ID");
        requireText(departmentName, "Department name");
        requireText(assignedBy, "Assigned by");
        Objects.requireNonNull(assignedAt, "Assigned at is required");

        return new AssetAssignment(
                UUID.randomUUID().toString(),
                assetId,
                itemId,
                assignedUserId,
                departmentName,
                assignedBy,
                assignedAt,
                remarks);
    }

    public void close(Instant returnedAt) {
        Objects.requireNonNull(returnedAt, "Returned at is required");
        if (!active) {
            throw new IllegalStateException("Assignment is already closed");
        }
        if (returnedAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("Returned at cannot be before assigned at");
        }
        this.returnedAt = returnedAt;
        this.active = false;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public String getAssignmentId() { return assignmentId; }
    public String getAssetId() { return assetId; }
    public String getItemId() { return itemId; }
    public String getAssignedUserId() { return assignedUserId; }
    public String getDepartmentName() { return departmentName; }
    public String getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getReturnedAt() { return returnedAt; }
    public boolean isActive() { return active; }
    public String getRemarks() { return remarks; }
    public Long getVersion() { return version; }
}
