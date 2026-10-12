package com.recnaile.reporting.service;

import com.recnaile.reporting.model.*;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class ReportingQueryService {
    private final ReportingProjectionRepository projections; private final ReportingFactRepository facts; private final MongoTemplate mongo;
    public ReportingQueryService(ReportingProjectionRepository projections, ReportingFactRepository facts, MongoTemplate mongo){this.projections=projections;this.facts=facts;this.mongo=mongo;}

    public Map<String,Object> dashboard(){ return dashboard(null, null); }

    /** KPI aggregation is performed only over this service's reporting projections/facts. */
    public Map<String,Object> dashboard(String departmentName, String projectId){
        Map<String,Object> k=new LinkedHashMap<>();
        long pendingVerification=countByProjection("INDENT","PENDING_VERIFICATION",departmentName,projectId);
        long pendingApproval=countByProjection("INDENT","PENDING_APPROVAL",departmentName,projectId);
        k.put("pendingIndents",pendingVerification+pendingApproval);
        Instant now=Instant.now();
        k.put("approvalAgingOver24Hours",countPendingOlderThan(now.minus(Duration.ofHours(24)),"PENDING_APPROVAL",departmentName,projectId));
        k.put("approvalAgingOver72Hours",countPendingOlderThan(now.minus(Duration.ofHours(72)),"PENDING_APPROVAL",departmentName,projectId));
        k.put("verificationAgingOver24Hours",countPendingOlderThan(now.minus(Duration.ofHours(24)),"PENDING_VERIFICATION",departmentName,projectId));
        k.put("indentsPendingVerification",pendingVerification);
        k.put("indentsPendingApproval",pendingApproval);
        k.put("approvedIndents",countByProjection("INDENT","APPROVED",departmentName,projectId));
        k.put("rejectedIndents",countByProjection("INDENT","REJECTED",departmentName,projectId));
        k.put("purchaseOrdersCreated",countByProjection("PURCHASE_ORDER","CREATED",departmentName,projectId));
        k.put("purchaseOrdersIssued",countByProjection("PURCHASE_ORDER","ISSUED",departmentName,projectId));
        k.put("purchaseOrdersClosed",countByProjection("PURCHASE_ORDER","CLOSED",departmentName,projectId));
        k.put("purchaseOrdersTotal",countByProjection("PURCHASE_ORDER",null,departmentName,projectId));
        k.put("purchaseOrderOrderedQuantity",sumField("PURCHASE_ORDER","orderedQuantity",departmentName,projectId));
        k.put("purchaseOrderEstimatedValue",sumField("PURCHASE_ORDER","estimatedValue",departmentName,projectId));
        k.put("inwardReceipts",countByProjection("INWARD",null,departmentName,projectId));
        k.put("acceptedInwardQuantity",sumField("INWARD","acceptedQuantity",departmentName,projectId));
        k.put("rejectedInwardQuantity",sumField("INWARD","rejectedQuantity",departmentName,projectId));
        k.put("lowStockItemsObserved",countByProjection("STOCK",null,departmentName,projectId));
        k.put("availableQuantityAtLowStockAlerts",sumField("STOCK","availableQuantity",departmentName,projectId));
        k.put("reorderThresholdForObservedLowStockItems",sumField("STOCK","reorderThreshold",departmentName,projectId));
        k.put("outwardIssues",countByProjection("OUTWARD",null,departmentName,projectId));
        k.put("outwardIssuedQuantity",sumField("OUTWARD","quantity",departmentName,projectId));
        k.put("returns",countByProjection("RETURN",null,departmentName,projectId));
        k.put("returnedQuantity",sumField("RETURN","quantity",departmentName,projectId));
        k.put("assets",countByProjection("ASSET",null,departmentName,projectId));
        k.put("slaBreaches",countFactsByType("SLA_BREACH",departmentName,projectId));
        k.put("filters",Map.of("departmentName",departmentName==null?"ALL":departmentName,"projectId",projectId==null?"ALL":projectId));
        k.put("generatedAt",Instant.now());
        k.put("freshness",freshness(departmentName,projectId));
        return k;
    }
    private long countByProjection(String entityType,String status,String departmentName,String projectId){
        Query q=projectionScope(entityType,departmentName,projectId);
        if(status!=null)q.addCriteria(Criteria.where("status").is(status));
        return mongo.count(q,ReportingProjection.class);
    }
    private long countFactsByType(String eventType,String departmentName,String projectId){
        Query q=new Query(Criteria.where("eventType").is(eventType));
        if(departmentName!=null&&!departmentName.isBlank())q.addCriteria(Criteria.where("departmentName").is(departmentName));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        return mongo.count(q,ReportingFact.class);
    }
    private Query projectionScope(String entityType,String departmentName,String projectId){
        Query q=new Query(Criteria.where("entityType").is(entityType));
        if(departmentName!=null&&!departmentName.isBlank())q.addCriteria(Criteria.where("departmentName").is(departmentName));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        return q;
    }
    private long countPendingOlderThan(Instant cutoff,String status,String departmentName,String projectId){Query q=projectionScope("INDENT",departmentName,projectId);q.addCriteria(Criteria.where("status").is(status).and("updatedAt").lt(cutoff));return mongo.count(q,ReportingProjection.class);}
    private long sumField(String entityType,String field,String departmentName,String projectId){
        // Build the aggregation match as Criteria. Aggregation.match does not accept
        // the org.bson.Document returned by Query.getQueryObject() in this Spring Data version.
        Criteria scope = Criteria.where("entityType").is(entityType);
        if(departmentName!=null&&!departmentName.isBlank())scope.and("departmentName").is(departmentName);
        if(projectId!=null&&!projectId.isBlank())scope.and("projectId").is(projectId);
        MatchOperation match=Aggregation.match(scope);
        GroupOperation group=Aggregation.group().sum(field).as("total");
        AggregationResults<org.bson.Document> result=mongo.aggregate(Aggregation.newAggregation(match,group),"reporting_projections",org.bson.Document.class);
        org.bson.Document doc=result.getUniqueMappedResult(); if(doc==null||doc.get("total")==null)return 0L;
        Object value=doc.get("total"); return value instanceof Number n?Math.round(n.doubleValue()):0L;
    }
    public ReportFreshness freshness(){
        return freshness(null, null);
    }

    /** Freshness follows the same scope as the dashboard, avoiding cross-scope counts. */
    public ReportFreshness freshness(String departmentName, String projectId){
        Query latestOccurredQuery = factScope(departmentName, projectId);
        latestOccurredQuery.with(Sort.by(Sort.Direction.DESC, "occurredAt"));
        ReportingFact latestOccurred = mongo.findOne(latestOccurredQuery, ReportingFact.class);
        Query latestIngestedQuery = factScope(departmentName, projectId);
        latestIngestedQuery.with(Sort.by(Sort.Direction.DESC, "ingestedAt"));
        ReportingFact latestIngested = mongo.findOne(latestIngestedQuery, ReportingFact.class);
        long count = mongo.count(factScope(departmentName, projectId), ReportingFact.class);
        return new ReportFreshness(
            latestOccurred == null ? null : latestOccurred.occurredAt(),
            latestIngested == null ? null : latestIngested.ingestedAt(),
            count,
            true
        );
    }

    private Query factScope(String departmentName, String projectId){
        Query q = new Query();
        if(departmentName!=null&&!departmentName.isBlank())q.addCriteria(Criteria.where("departmentName").is(departmentName));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        return q;
    }
    public Page<ReportingProjection> projections(String entityType,String status,String departmentName,String projectId,Pageable pageable){
        Query q=new Query();
        if(entityType!=null&&!entityType.isBlank())q.addCriteria(Criteria.where("entityType").is(entityType.toUpperCase(Locale.ROOT)));
        if(status!=null&&!status.isBlank())q.addCriteria(Criteria.where("status").is(status.toUpperCase(Locale.ROOT)));
        if(departmentName!=null&&!departmentName.isBlank())q.addCriteria(Criteria.where("departmentName").is(departmentName));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        long total=mongo.count(q,ReportingProjection.class); q.with(pageable);
        return new PageImpl<>(mongo.find(q,ReportingProjection.class),pageable,total);
    }
    public Page<ReportingFact> facts(String eventType,Instant from,Instant to,String departmentName,String projectId,Pageable pageable){
        Query q=new Query();
        if(eventType!=null&&!eventType.isBlank())q.addCriteria(Criteria.where("eventType").is(eventType));
        if(departmentName!=null&&!departmentName.isBlank())q.addCriteria(Criteria.where("departmentName").is(departmentName));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        if(from!=null||to!=null){Criteria date=Criteria.where("occurredAt");if(from!=null&&to!=null)date.gte(from).lte(to);else if(from!=null)date.gte(from);else date.lte(to);q.addCriteria(date);}
        long total=mongo.count(q,ReportingFact.class);q.with(pageable);
        return new PageImpl<>(mongo.find(q,ReportingFact.class),pageable,total);
    }
    public Map<String,Object> periodSummary(ReportPeriod period, LocalDate from, LocalDate to) {
        return periodSummary(period, from, to, null, null);
    }

    /** Period aggregation is restricted to reporting facts in the caller's resolved scope. */
    public Map<String,Object> periodSummary(ReportPeriod period, LocalDate from, LocalDate to,
                                            String departmentName, String projectId) {
        if (period == null) throw new IllegalArgumentException("period is required");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = from == null ? defaultStart(period, today) : from;
        LocalDate end = to == null ? today : to;
        if (end.isBefore(start)) throw new IllegalArgumentException("to must be on or after from");
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        if (days > 3660) throw new IllegalArgumentException("Report range cannot exceed 10 years");
        Instant startAt = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endAt = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
        Criteria criteria = Criteria.where("occurredAt").gte(startAt).lte(endAt);
        if (departmentName != null && !departmentName.isBlank()) criteria.and("departmentName").is(departmentName);
        if (projectId != null && !projectId.isBlank()) criteria.and("projectId").is(projectId);
        Query scopedFacts = new Query(criteria).with(Sort.by(Sort.Direction.ASC, "occurredAt"));
        List<ReportingFact> data = mongo.find(scopedFacts, ReportingFact.class);
        Map<String,Long> counts = new TreeMap<>();
        Map<String,Long> quantities = new TreeMap<>();
        Map<LocalDate,Map<String,Object>> buckets = new TreeMap<>();
        for (ReportingFact f : data) {
            counts.merge(f.eventType(), 1L, Long::sum);
            if (f.quantity() != null) quantities.merge(f.eventType(), f.quantity(), Long::sum);
            if (f.acceptedQuantity() != null) quantities.merge("GOODS_RECEIVED_ACCEPTED", f.acceptedQuantity(), Long::sum);
            if (f.rejectedQuantity() != null) quantities.merge("GOODS_RECEIVED_REJECTED", f.rejectedQuantity(), Long::sum);
            LocalDate bucket = bucketStart(f.occurredAt().atZone(ZoneOffset.UTC).toLocalDate(), period);
            @SuppressWarnings("unchecked") Map<String,Long> bucketCounts = (Map<String,Long>) buckets.computeIfAbsent(bucket, d -> {
                Map<String,Object> b = new LinkedHashMap<>();
                b.put("eventCounts", new TreeMap<String,Long>());
                b.put("quantityTotals", new TreeMap<String,Long>());
                return b;
            }).get("eventCounts");
            bucketCounts.merge(f.eventType(), 1L, Long::sum);
            @SuppressWarnings("unchecked") Map<String,Long> bucketQuantities = (Map<String,Long>) buckets.get(bucket).get("quantityTotals");
            if (f.quantity() != null) bucketQuantities.merge(f.eventType(), f.quantity(), Long::sum);
            if (f.acceptedQuantity() != null) bucketQuantities.merge("GOODS_RECEIVED_ACCEPTED", f.acceptedQuantity(), Long::sum);
            if (f.rejectedQuantity() != null) bucketQuantities.merge("GOODS_RECEIVED_REJECTED", f.rejectedQuantity(), Long::sum);
        }
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("period", period); response.put("from", start); response.put("to", end); response.put("timezone", "UTC");
        response.put("eventCounts", counts); response.put("quantityTotals", quantities); response.put("buckets", buckets);
        response.put("filters", Map.of("departmentName", departmentName == null ? "ALL" : departmentName,
            "projectId", projectId == null ? "ALL" : projectId));
        response.put("generatedAt", Instant.now()); response.put("freshness", freshness(departmentName, projectId));
        return response;
    }
    private LocalDate bucketStart(LocalDate date,ReportPeriod period){return switch(period){case DAILY->date;case WEEKLY->date.with(DayOfWeek.MONDAY);case MONTHLY->date.withDayOfMonth(1);case QUARTERLY->LocalDate.of(date.getYear(),((date.getMonthValue()-1)/3)*3+1,1);case HALF_YEARLY->LocalDate.of(date.getYear(),date.getMonthValue()<=6?1:7,1);case YEARLY->date.withDayOfYear(1);};}
    private LocalDate defaultStart(ReportPeriod p,LocalDate now){return bucketStart(now,p);}
}
