package com.recnaile.asset.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Immutable-in-history assignment record for one asset lifecycle assignment.
 * The record is closed when the asset is returned; it is never deleted.
 */
@Document(collection = "asset_assignments")
@CompoundIndex(name = "asset_assignment_timeline_idx", def = "{ 'assetId': 1, 'assignedAt': -1 }")
@CompoundIndex(name = "employee_assignment_timeline_idx", def = "{ 'employeeId': 1, 'assignedAt': -1 }")
@CompoundIndex(name = "department_assignment_timeline_idx", def = "{ 'departmentName': 1, 'assignedAt': -1 }")
@CompoundIndex(name = "active_asset_assignment_unique_idx", def = "{ 'assetId': 1, 'returnedAt': 1 }", unique = true)
public class AssetAssignment {

    @Id
    private String assignmentId;

    @Indexed
    private String assetId;

    @Indexed
    private String employeeId;

    @Indexed
    private String departmentName;

    private String issuedBy;
    private String outwardId;
    private Instant assignedAt;
    private Instant returnedAt;
    private String returnId;
    private AssetCondition returnCondition;
    private String remarks;

    @Version
    private Long version;

    protected AssetAssignment() {
        // Required by Spring Data MongoDB.
    }

    private AssetAssignment(
            String assignmentId,
            String assetId,
            String employeeId,
            String departmentName,
            String issuedBy,
            String outwardId,
            Instant assignedAt,
            String remarks) {
        this.assignmentId = assignmentId;
        this.assetId = assetId;
        this.employeeId = employeeId;
        this.departmentName = departmentName;
        this.issuedBy = issuedBy;
        this.outwardId = outwardId;
        this.assignedAt = assignedAt;
        this.remarks = remarks;
    }

    public static AssetAssignment start(
            String assetId,
            String employeeId,
            String departmentName,
            String issuedBy,
            String outwardId,
            Instant assignedAt,
            String remarks) {
        requireText(assetId, "Asset ID");
        requireText(employeeId, "Employee ID");
        requireText(departmentName, "Department name");
        requireText(issuedBy, "Issuer ID");
        requireText(outwardId, "Outward ID");
        requireTimestamp(assignedAt);

        return new AssetAssignment(
                UUID.randomUUID().toString(),
                assetId,
                employeeId,
                departmentName,
                issuedBy,
                outwardId,
                assignedAt,
                remarks);
    }

    /** Closes this historical assignment exactly once. */
    public void close(
            Instant returnedAt,
            String returnId,
            AssetCondition returnCondition) {
        requireTimestamp(returnedAt);
        requireText(returnId, "Return ID");
        if (this.returnedAt != null) {
            throw new IllegalStateException("Assignment is already closed");
        }
        if (returnedAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("Return time cannot be before assignment time");
        }
        if (returnCondition == null) {
            throw new IllegalArgumentException("Return condition is required");
        }

        this.returnedAt = returnedAt;
        this.returnId = returnId;
        this.returnCondition = returnCondition;
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

    public String getAssignmentId() { return assignmentId; }
    public String getAssetId() { return assetId; }
    public String getEmployeeId() { return employeeId; }
    public String getDepartmentName() { return departmentName; }
    public String getIssuedBy() { return issuedBy; }
    public String getOutwardId() { return outwardId; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getReturnedAt() { return returnedAt; }
    public String getReturnId() { return returnId; }
    public AssetCondition getReturnCondition() { return returnCondition; }
    public String getRemarks() { return remarks; }
    public boolean isActive() { return returnedAt == null; }
    public Long getVersion() { return version; }
}
