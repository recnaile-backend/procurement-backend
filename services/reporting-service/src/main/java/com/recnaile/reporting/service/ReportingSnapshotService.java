package com.recnaile.reporting.service;

import com.recnaile.reporting.model.ReportFreshness;
import com.recnaile.reporting.model.ReportingSnapshot;
import com.recnaile.reporting.repository.ReportingSnapshotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Map;

@Service
public class ReportingSnapshotService {
    private final ReportingQueryService query;
    private final ReportingSnapshotRepository snapshots;
    private final MongoTemplate mongo;

    public ReportingSnapshotService(ReportingQueryService query, ReportingSnapshotRepository snapshots, MongoTemplate mongo) {
        this.query = query; this.snapshots = snapshots; this.mongo = mongo;
    }

    /** Captures a platform-wide daily snapshot. Scoped snapshots can be captured by an authorized reporting administrator. */
    @Scheduled(cron="${app.reporting.snapshot-cron:0 5 0 * * *}", zone="UTC")
    public void captureDailySnapshot() { capture(LocalDate.now(ZoneOffset.UTC), null, null); }

    public ReportingSnapshot capture(LocalDate date) { return capture(date, null, null); }

    public ReportingSnapshot capture(LocalDate date, String departmentId, String projectId) {
        if (date == null) throw new IllegalArgumentException("snapshot date is required");
        Map<String,Object> k = query.dashboard(departmentId, projectId);
        ReportFreshness freshness = (ReportFreshness) k.get("freshness");
        String id = ReportingSnapshot.keyFor(date, departmentId, projectId);
        ReportingSnapshot snapshot = new ReportingSnapshot(id, date, departmentId, projectId, Instant.now(),
            number(k,"pendingIndents").longValue(), number(k,"indentsPendingVerification").longValue(),
            number(k,"indentsPendingApproval").longValue(), number(k,"approvedIndents").longValue(),
            number(k,"rejectedIndents").longValue(), number(k,"purchaseOrdersCreated").longValue(),
            number(k,"purchaseOrdersIssued").longValue(), number(k,"purchaseOrdersClosed").longValue(),
            number(k,"inwardReceipts").longValue(), number(k,"acceptedInwardQuantity").longValue(),
            number(k,"rejectedInwardQuantity").longValue(), number(k,"lowStockItemsObserved").longValue(),
            number(k,"availableQuantityAtLowStockAlerts").longValue(),
            number(k,"reorderThresholdForObservedLowStockItems").longValue(), number(k,"outwardIssues").longValue(),
            number(k,"outwardIssuedQuantity").longValue(), number(k,"returns").longValue(),
            number(k,"returnedQuantity").longValue(), number(k,"assets").longValue(),
            number(k,"slaBreaches").longValue(), freshness.lastEventOccurredAt(), freshness.lastUpdatedAt());
        return snapshots.save(snapshot);
    }

    /** Returns snapshots for exactly the caller's resolved scope; unscoped snapshots are never mixed with scoped ones. */
    public Page<ReportingSnapshot> findSnapshots(String departmentId, String projectId, Pageable pageable) {
        Query q = new Query();
        q.addCriteria(Criteria.where("departmentId").is(departmentId));
        q.addCriteria(Criteria.where("projectId").is(projectId));
        long total = mongo.count(q, ReportingSnapshot.class);
        q.with(pageable);
        return new PageImpl<>(mongo.find(q, ReportingSnapshot.class), pageable, total);
    }

    private Number number(Map<String,Object> map, String key) {
        Object value = map.get(key); return value instanceof Number n ? n : 0L;
    }
}
