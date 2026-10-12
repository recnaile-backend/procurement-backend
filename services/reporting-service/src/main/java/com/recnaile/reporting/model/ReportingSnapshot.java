package com.recnaile.reporting.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDate;

/** Point-in-time KPI snapshot; preserves values that mutable projections cannot reconstruct later. */
@Document("reporting_snapshots")
public record ReportingSnapshot(
    @Id String id,
    @Indexed(unique = true) LocalDate snapshotDate,
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
    double acceptedInwardQuantity,
    double rejectedInwardQuantity,
    long lowStockItemsObserved,
    double availableQuantityAtLowStockAlerts,
    double reorderThresholdForObservedLowStockItems,
    long outwardIssues,
    double outwardIssuedQuantity,
    long returns,
    double returnedQuantity,
    long assets,
    long slaBreaches,
    Instant lastEventOccurredAt,
    Instant lastUpdatedAt
) {}
