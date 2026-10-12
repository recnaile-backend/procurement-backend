package com.recnaile.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDate;

/** Immutable point-in-time KPI snapshot, keyed by date and reporting scope. */
@Document("reporting_snapshots")
@CompoundIndex(name = "snapshot_date_scope_unique", def = "{'snapshotDate':1,'departmentName':1,'projectId':1}", unique = true)
@CompoundIndex(name = "snapshot_scope_date", def = "{'departmentName':1,'projectId':1,'snapshotDate':-1}")
public record ReportingSnapshot(
    @Id String id,
    @Indexed LocalDate snapshotDate,
    String departmentName,
    String projectId,
    Instant capturedAt,
    long pendingIndents,
    long indentsPendingVerification,
    long indentsPendingApproval,
    long approvedIndents,
    long rejectedIndents,
    long purchaseOrdersCreated,
    long purchaseOrdersIssued,
    long purchaseOrdersClosed,
    long inwardReceipts,
    long acceptedInwardQuantity,
    long rejectedInwardQuantity,
    long lowStockItemsObserved,
    long availableQuantityAtLowStockAlerts,
    long reorderThresholdForObservedLowStockItems,
    long outwardIssues,
    long outwardIssuedQuantity,
    long returns,
    long returnedQuantity,
    long assets,
    long slaBreaches,
    Instant lastEventOccurredAt,
    Instant lastUpdatedAt
) {
    public static String keyFor(LocalDate date, String departmentName, String projectId) {
        return departmentName == null && projectId == null ? date.toString() : date + "|" + (departmentName == null ? "ALL" : departmentName) + "|" + (projectId == null ? "ALL" : projectId);
    }
}
