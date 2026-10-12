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

    public Map<String,Object> dashboard(){
        Map<String,Object> k=new LinkedHashMap<>();
        k.put("pendingIndents",projections.countByEntityTypeAndStatus("INDENT","PENDING_VERIFICATION")+projections.countByEntityTypeAndStatus("INDENT","PENDING_APPROVAL"));
        Instant now=Instant.now();
        k.put("approvalAgingOver24Hours",countPendingOlderThan(now.minus(Duration.ofHours(24)),"PENDING_APPROVAL"));
        k.put("approvalAgingOver72Hours",countPendingOlderThan(now.minus(Duration.ofHours(72)),"PENDING_APPROVAL"));
        k.put("verificationAgingOver24Hours",countPendingOlderThan(now.minus(Duration.ofHours(24)),"PENDING_VERIFICATION"));
        k.put("indentsPendingVerification",projections.countByEntityTypeAndStatus("INDENT","PENDING_VERIFICATION"));
        k.put("indentsPendingApproval",projections.countByEntityTypeAndStatus("INDENT","PENDING_APPROVAL"));
        k.put("approvedIndents",projections.countByEntityTypeAndStatus("INDENT","APPROVED"));
        k.put("rejectedIndents",projections.countByEntityTypeAndStatus("INDENT","REJECTED"));
        k.put("purchaseOrdersCreated",projections.countByEntityTypeAndStatus("PURCHASE_ORDER","CREATED"));
        k.put("purchaseOrdersIssued",projections.countByEntityTypeAndStatus("PURCHASE_ORDER","ISSUED"));
        k.put("purchaseOrdersClosed",projections.countByEntityTypeAndStatus("PURCHASE_ORDER","CLOSED"));
        k.put("inwardReceipts",projections.countByEntityType("INWARD")); k.put("acceptedInwardQuantity",sumField("INWARD","acceptedQuantity")); k.put("rejectedInwardQuantity",sumField("INWARD","rejectedQuantity"));
        k.put("lowStockItemsObserved",projections.countByEntityType("STOCK")); k.put("availableQuantityAtLowStockAlerts",sumField("STOCK","availableQuantity")); k.put("reorderThresholdForObservedLowStockItems",sumField("STOCK","reorderThreshold"));
        k.put("outwardIssues",projections.countByEntityType("OUTWARD")); k.put("outwardIssuedQuantity",sumField("OUTWARD","quantity")); k.put("returns",projections.countByEntityType("RETURN")); k.put("returnedQuantity",sumField("RETURN","quantity"));
        k.put("assets",projections.countByEntityType("ASSET")); k.put("slaBreaches",facts.countByEventType("SLA_BREACH"));
        k.put("freshness",freshness()); return k;
    }
    private long countPendingOlderThan(Instant cutoff,String status){Query q=new Query(Criteria.where("entityType").is("INDENT").and("status").is(status).and("updatedAt").lt(cutoff));return mongo.count(q,ReportingProjection.class);}
    private double sumField(String entityType,String field){
        MatchOperation match=Aggregation.match(Criteria.where("entityType").is(entityType));
        GroupOperation group=Aggregation.group().sum(field).as("total");
        AggregationResults<org.bson.Document> result=mongo.aggregate(Aggregation.newAggregation(match,group),"reporting_projections",org.bson.Document.class);
        org.bson.Document doc=result.getUniqueMappedResult(); if(doc==null||doc.get("total")==null)return 0d;
        Object value=doc.get("total"); return value instanceof Number n?n.doubleValue():0d;
    }
    public ReportFreshness freshness(){ReportingFact latestIngested=facts.findFirstByOrderByIngestedAtDesc().orElse(null); ReportingFact latestOccurred=facts.findFirstByOrderByOccurredAtDesc().orElse(null); return new ReportFreshness(latestOccurred==null?null:latestOccurred.occurredAt(),latestIngested==null?null:latestIngested.ingestedAt(),facts.count(),true);}
    public Page<ReportingProjection> projections(String entityType,String status,String departmentId,String projectId,Pageable pageable){
        Query q=new Query();
        if(entityType!=null&&!entityType.isBlank())q.addCriteria(Criteria.where("entityType").is(entityType.toUpperCase(Locale.ROOT)));
        if(status!=null&&!status.isBlank())q.addCriteria(Criteria.where("status").is(status.toUpperCase(Locale.ROOT)));
        if(departmentId!=null&&!departmentId.isBlank())q.addCriteria(Criteria.where("departmentId").is(departmentId));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        long total=mongo.count(q,ReportingProjection.class); q.with(pageable);
        return new PageImpl<>(mongo.find(q,ReportingProjection.class),pageable,total);
    }
    public Page<ReportingFact> facts(String eventType,Instant from,Instant to,String departmentId,String projectId,Pageable pageable){
        Query q=new Query();
        if(eventType!=null&&!eventType.isBlank())q.addCriteria(Criteria.where("eventType").is(eventType));
        if(departmentId!=null&&!departmentId.isBlank())q.addCriteria(Criteria.where("departmentId").is(departmentId));
        if(projectId!=null&&!projectId.isBlank())q.addCriteria(Criteria.where("projectId").is(projectId));
        if(from!=null||to!=null){Criteria date=Criteria.where("occurredAt");if(from!=null&&to!=null)date.gte(from).lte(to);else if(from!=null)date.gte(from);else date.lte(to);q.addCriteria(date);}
        long total=mongo.count(q,ReportingFact.class);q.with(pageable);
        return new PageImpl<>(mongo.find(q,ReportingFact.class),pageable,total);
    }
    public Map<String,Object> periodSummary(ReportPeriod period,LocalDate from,LocalDate to){
        LocalDate start=from==null?defaultStart(period,LocalDate.now()):from; LocalDate end=to==null?LocalDate.now():to;
        if(end.isBefore(start))throw new IllegalArgumentException("to must be on or after from");
        Instant startAt=start.atStartOfDay(ZoneOffset.UTC).toInstant(), endAt=end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
        List<ReportingFact> data=facts.findByOccurredAtBetween(startAt,endAt); Map<String,Long> counts=new TreeMap<>(); Map<String,Double> quantities=new TreeMap<>();
        Map<LocalDate,Map<String,Object>> buckets=new TreeMap<>();
        for(ReportingFact f:data){
            counts.merge(f.eventType(),1L,Long::sum);
            if(f.quantity()!=null)quantities.merge(f.eventType(),f.quantity(),Double::sum);
            if(f.acceptedQuantity()!=null)quantities.merge("GOODS_RECEIVED_ACCEPTED",f.acceptedQuantity(),Double::sum);
            if(f.rejectedQuantity()!=null)quantities.merge("GOODS_RECEIVED_REJECTED",f.rejectedQuantity(),Double::sum);
            LocalDate bucket=bucketStart(f.occurredAt().atZone(ZoneOffset.UTC).toLocalDate(),period);
            @SuppressWarnings("unchecked") Map<String,Long> bucketCounts=(Map<String,Long>)buckets.computeIfAbsent(bucket,d->{Map<String,Object> b=new LinkedHashMap<>();b.put("eventCounts",new TreeMap<String,Long>());b.put("quantityTotals",new TreeMap<String,Double>());return b;}).get("eventCounts");
            bucketCounts.merge(f.eventType(),1L,Long::sum);
            @SuppressWarnings("unchecked") Map<String,Double> bucketQuantities=(Map<String,Double>)buckets.get(bucket).get("quantityTotals");
            if(f.quantity()!=null)bucketQuantities.merge(f.eventType(),f.quantity(),Double::sum);
            if(f.acceptedQuantity()!=null)bucketQuantities.merge("GOODS_RECEIVED_ACCEPTED",f.acceptedQuantity(),Double::sum);
            if(f.rejectedQuantity()!=null)bucketQuantities.merge("GOODS_RECEIVED_REJECTED",f.rejectedQuantity(),Double::sum);
        }
        return Map.of("period",period,"from",start,"to",end,"timezone","UTC","eventCounts",counts,"quantityTotals",quantities,"buckets",buckets,"freshness",freshness());
    }
    private LocalDate bucketStart(LocalDate date,ReportPeriod period){return switch(period){case DAILY->date;case WEEKLY->date.with(DayOfWeek.MONDAY);case MONTHLY->date.withDayOfMonth(1);case QUARTERLY->LocalDate.of(date.getYear(),((date.getMonthValue()-1)/3)*3+1,1);case HALF_YEARLY->LocalDate.of(date.getYear(),date.getMonthValue()<=6?1:7,1);case YEARLY->date.withDayOfYear(1);};}
    private LocalDate defaultStart(ReportPeriod p,LocalDate now){return bucketStart(now,p);}
}
