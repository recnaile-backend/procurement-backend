package com.recnaile.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.Optional;

@Service
public class ReportingEventProcessor {
    private final ObjectMapper mapper;
    private final ReportingFactRepository facts;
    private final ReportingProjectionRepository projections;
    private final MeterRegistry meters;

    public ReportingEventProcessor(ObjectMapper mapper, ReportingFactRepository facts, ReportingProjectionRepository projections, MeterRegistry meters) {
        this.mapper = mapper; this.facts = facts; this.projections = projections; this.meters = meters;
    }

    public void process(String raw) {
        try {
            JsonNode envelope = mapper.readTree(raw);
            new ReportingEventContractValidator().validate(envelope);
            String eventId = text(envelope, "eventId");
            String eventType = text(envelope, "eventType");
            if (eventId == null || eventType == null || !envelope.hasNonNull("payload")) throw new IllegalArgumentException("Event envelope requires eventId, eventType and payload");
            Instant occurredAt = instant(text(envelope, "occurredAt"));
            if (eventId.isBlank() || eventType.isBlank() || !envelope.hasNonNull("producer") || !envelope.path("eventVersion").canConvertToInt() || envelope.path("eventVersion").asInt() < 1) throw new IllegalArgumentException("Event envelope has invalid eventId, eventType, eventVersion or producer");
            JsonNode payload = envelope.path("payload");
            ReportingFact fact = mapFact(eventId, eventType, occurredAt, text(envelope,"producer"), text(envelope,"correlationId"), text(envelope,"traceId"), payload);
            try { facts.insert(fact); meters.counter("reporting.events.processed", "event_type", eventType).increment(); }
            catch (DuplicateKeyException duplicate) { meters.counter("reporting.events.duplicate", "event_type", eventType).increment(); }
            updateProjection(fact, payload);
        } catch (RuntimeException e) { meters.counter("reporting.events.failed").increment(); throw e; }
        catch (Exception e) { meters.counter("reporting.events.failed").increment(); throw new IllegalArgumentException("Invalid reporting event JSON", e); }
    }

    private ReportingFact mapFact(String eventId, String type, Instant occurred, String producer, String correlation, String trace, JsonNode p) {
        String entityType = null, entityId = null, related = null, status = null, condition = null, actor = null;
        switch (type) {
            case "INDENT_SUBMITTED", "INDENT_VERIFIED", "INDENT_APPROVED", "INDENT_REJECTED" -> { entityType="INDENT"; entityId=text(p,"indentId"); }
            case "PO_CREATED", "PO_ISSUED", "PO_CLOSED" -> { entityType="PURCHASE_ORDER"; entityId=text(p,"poId"); related=text(p,"indentId"); }
            case "GOODS_RECEIVED", "REJECTION_FLAGGED" -> { entityType="INWARD"; entityId=text(p,"grnId"); related=text(p,"poId"); }
            case "LOW_STOCK_ALERT" -> { entityType="STOCK"; entityId=text(p,"itemId"); }
            case "OUTWARD_ISSUED" -> { entityType="OUTWARD"; entityId=text(p,"outwardId"); }
            case "RETURN_RECORDED" -> { entityType="RETURN"; entityId=text(p,"returnId"); condition=text(p,"condition"); }
            case "ASSET_STATUS_CHANGED" -> { entityType="ASSET"; entityId=text(p,"assetId"); status=text(p,"newStatus"); }
            case "ASSET_CLOSURE_REQUESTED", "ASSET_CREATION_REQUESTED" -> { entityType="ASSET_REQUEST"; entityId=eventId; related=text(p,"assetId"); }
            case "SLA_BREACH" -> { entityType="SLA_BREACH"; entityId=eventId; related=text(p,"entityId"); status="BREACHED"; }
            case "AUDIT_EVENT" -> { entityType=text(p,"entityType"); entityId=text(p,"entityId"); actor=text(p,"actorId"); status=text(p,"afterStatus"); }
            default -> { entityType="UNMAPPED"; entityId=eventId; }
        }
        if (entityId == null || entityId.isBlank()) entityId = eventId;
        if (status == null) status = switch (type) {
            case "INDENT_SUBMITTED" -> "PENDING_VERIFICATION"; case "INDENT_VERIFIED" -> "PENDING_APPROVAL"; case "INDENT_APPROVED" -> "APPROVED"; case "INDENT_REJECTED" -> "REJECTED";
            case "PO_CREATED" -> "CREATED"; case "PO_ISSUED" -> "ISSUED"; case "PO_CLOSED" -> "CLOSED";
            case "GOODS_RECEIVED" -> "RECEIVED"; case "RETURN_RECORDED" -> condition; default -> null;
        };
        return new ReportingFact(eventId,type,occurred,producer,entityType,entityId,related,text(p,"departmentId"),text(p,"projectId"),text(p,"itemId"),text(p,"vendorId"),status,condition,actor,
            number(p,"quantity"),number(p,"orderedQuantity"),number(p,"acceptedQuantity"),number(p,"rejectedQuantity"),number(p,"estimatedValue"),number(p,"unitPrice"),number(p,"slaWindowHours"),number(p,"overdueSinceHours"),correlation,trace,Instant.now());
    }

    private void updateProjection(ReportingFact f, JsonNode p) {
        if ("UNMAPPED".equals(f.entityType())) return;
        String id = ReportingProjection.keyFor(f.entityType(), f.entityId());
        Optional<ReportingProjection> existing = projections.findById(id);
        if (existing.isPresent() && existing.get().sourceOccurredAt()!=null && f.occurredAt()!=null && existing.get().sourceOccurredAt().isAfter(f.occurredAt())) return;
        ReportingProjection old = existing.orElse(null);
        String status = f.status()!=null ? f.status() : old==null ? null : old.status();
        String dept = f.departmentId()!=null ? f.departmentId() : old==null ? null : old.departmentId();
        String project = f.projectId()!=null ? f.projectId() : old==null ? null : old.projectId();
        String item = f.itemId()!=null ? f.itemId() : old==null ? null : old.itemId();
        String vendor = f.vendorId()!=null ? f.vendorId() : old==null ? null : old.vendorId();
        String related = f.relatedEntityId()!=null ? f.relatedEntityId() : old==null ? null : old.relatedEntityId();
        Double qty = first(number(p,"quantity"), old==null?null:old.quantity());
        Double est = first(number(p,"estimatedValue"), old==null?null:old.estimatedValue());
        Double ordered = first(number(p,"orderedQuantity"), old==null?null:old.orderedQuantity());
        Double accepted = first(number(p,"acceptedQuantity"), old==null?null:old.acceptedQuantity());
        Double rejected = first(number(p,"rejectedQuantity"), old==null?null:old.rejectedQuantity());
        Double available = first(number(p,"availableQuantity"), old==null?null:old.availableQuantity());
        Double threshold = first(number(p,"reorderThreshold"), old==null?null:old.reorderThreshold());
        String condition = f.condition()!=null?f.condition():old==null?null:old.condition();
        String actor = first(f.actorId(), text(p,"approvedBy"), text(p,"verifiedBy"), text(p,"issuedBy"), text(p,"receivedBy"), text(p,"returnedBy"), old==null?null:old.actorId());
        projections.save(new ReportingProjection(id,f.entityType(),f.entityId(),status,dept,project,item,vendor,related,condition,qty,est,ordered,accepted,rejected,available,threshold,actor,f.producer(),old==null?f.occurredAt():old.createdAt(),Instant.now(),f.occurredAt(),f.eventId(),f.eventType()));
    }
    private static String text(JsonNode n,String k){JsonNode v=n==null?null:n.get(k);return v==null||v.isNull()?null:v.asText();}
    private static Double number(JsonNode n,String k){JsonNode v=n==null?null:n.get(k);return v==null||v.isNull()||!v.isNumber()?null:v.doubleValue();}
    private static Instant instant(String s){try{if(s==null||s.isBlank())throw new IllegalArgumentException("occurredAt is required");return Instant.parse(s);}catch(Exception e){throw new IllegalArgumentException("occurredAt must be an ISO-8601 instant",e);}}
    private static Double first(Double a,Double b){return a!=null?a:b;}
    private static String first(String... values){for(String v:values)if(v!=null&&!v.isBlank())return v;return null;}
}
