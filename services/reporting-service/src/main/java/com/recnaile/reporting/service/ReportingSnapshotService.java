package com.recnaile.reporting.service;

import com.recnaile.reporting.model.ReportFreshness;
import com.recnaile.reporting.model.ReportingSnapshot;
import com.recnaile.reporting.repository.ReportingSnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.Map;

@Service
public class ReportingSnapshotService {
    private final ReportingQueryService query;
    private final ReportingSnapshotRepository snapshots;
    public ReportingSnapshotService(ReportingQueryService query, ReportingSnapshotRepository snapshots){this.query=query;this.snapshots=snapshots;}

    @Scheduled(cron="${app.reporting.snapshot-cron:0 5 0 * * *}", zone="UTC")
    public void captureDailySnapshot(){capture(LocalDate.now(ZoneOffset.UTC));}

    public ReportingSnapshot capture(LocalDate date){
        Map<String,Object> k=query.dashboard(); ReportFreshness freshness=(ReportFreshness)k.get("freshness");
        ReportingSnapshot snapshot=new ReportingSnapshot(date.toString(),date,Instant.now(),number(k,"pendingIndents").longValue(),number(k,"indentsPendingVerification").longValue(),number(k,"indentsPendingApproval").longValue(),number(k,"approvedIndents").longValue(),number(k,"rejectedIndents").longValue(),number(k,"purchaseOrdersCreated").longValue(),number(k,"purchaseOrdersIssued").longValue(),number(k,"purchaseOrdersClosed").longValue(),number(k,"inwardReceipts").longValue(),number(k,"acceptedInwardQuantity").doubleValue(),number(k,"rejectedInwardQuantity").doubleValue(),number(k,"lowStockItemsObserved").longValue(),number(k,"availableQuantityAtLowStockAlerts").doubleValue(),number(k,"reorderThresholdForObservedLowStockItems").doubleValue(),number(k,"outwardIssues").longValue(),number(k,"outwardIssuedQuantity").doubleValue(),number(k,"returns").longValue(),number(k,"returnedQuantity").doubleValue(),number(k,"assets").longValue(),number(k,"slaBreaches").longValue(),freshness.lastEventOccurredAt(),freshness.lastUpdatedAt());
        return snapshots.save(snapshot);
    }
    private Number number(Map<String,Object> map,String key){Object value=map.get(key);return value instanceof Number n?n:0L;}
}
