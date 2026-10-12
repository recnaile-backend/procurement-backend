package com.recnaile.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
            case "LOW_STOCK_ALERT" -> { entityType="STOCK"; entityId=text(p,"itemId"); status="LOW_STOCK"; }
            case "OUTWARD_ISSUED" -> { entityType="OUTWARD"; entityId=text(p,"outwardId"); }
            case "RETURN_RECORDED" -> { entityType="RETURN"; entityId=text(p,"returnId"); related=text(p,"assetId"); condition=text(p,"condition"); }
            case "ASSET_STATUS_CHANGED" -> { entityType="ASSET"; entityId=text(p,"assetId"); status=text(p,"newStatus"); }
            case "ASSET_CREATION_REQUESTED" -> { entityType="ASSET_REQUEST"; entityId=eventId; related=text(p,"outwardId"); status="CREATION_REQUESTED"; }
            case "ASSET_CLOSURE_REQUESTED" -> { entityType="ASSET"; entityId=text(p,"assetId"); related=text(p,"returnId"); status="CLOSURE_REQUESTED"; }
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
        Long quantity = number(p, "quantity");
        if ("GOODS_RECEIVED".equals(type) && quantity == null) quantity = number(p, "acceptedQuantity");
        Long orderedQuantity = number(p, "orderedQuantity");
        Long estimatedValue = number(p, "estimatedValue");
        if ("PO_CREATED".equals(type)) {
            orderedQuantity = sumItemQuantities(p.path("items"));
            estimatedValue = sumItemValues(p.path("items"));
        }
        return new ReportingFact(eventId,type,occurred,producer,entityType,entityId,related,text(p,"departmentId"),text(p,"projectId"),text(p,"itemId"),text(p,"vendorId"),status,condition,actor,
            quantity,orderedQuantity,number(p,"acceptedQuantity"),number(p,"rejectedQuantity"),estimatedValue,number(p,"unitPrice"),number(p,"slaWindowHours"),number(p,"overdueSinceHours"),correlation,trace,Instant.now());
    }

    /** Rebuilds a projection from a retained immutable fact without reinserting the fact. */
    public void rebuildProjectionFromFact(ReportingFact fact) {
        if (fact == null || fact.entityType() == null || "UNMAPPED".equals(fact.entityType())) return;
        ObjectNode payload = mapper.createObjectNode();
        put(payload, "quantity", fact.quantity());
        put(payload, "acceptedQuantity", fact.acceptedQuantity());
        put(payload, "rejectedQuantity", fact.rejectedQuantity());
        put(payload, "orderedQuantity", fact.orderedQuantity());
        put(payload, "estimatedValue", fact.estimatedValue());
        put(payload, "unitPrice", fact.unitPrice());
        put(payload, "departmentId", fact.departmentId());
        put(payload, "projectId", fact.projectId());
        put(payload, "itemId", fact.itemId());
        put(payload, "vendorId", fact.vendorId());
        put(payload, "condition", fact.condition());
        updateProjection(fact, payload);
    }

    private static void put(ObjectNode node, String field, String value) {
        if (value != null) node.put(field, value);
    }
    private static void put(ObjectNode node, String field, Long value) {
        if (value != null) node.put(field, value);
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
        Long qty = first(f.quantity(), number(p,"quantity"), old==null?null:old.quantity());
        Long est = first(f.estimatedValue(), number(p,"estimatedValue"), old==null?null:old.estimatedValue());
        Long ordered = first(f.orderedQuantity(), number(p,"orderedQuantity"), old==null?null:old.orderedQuantity());
        Long accepted = first(f.acceptedQuantity(), number(p,"acceptedQuantity"), old==null?null:old.acceptedQuantity());
        Long rejected = first(f.rejectedQuantity(), number(p,"rejectedQuantity"), old==null?null:old.rejectedQuantity());
        Long available = first(number(p,"availableQuantity"), old==null?null:old.availableQuantity());
        Long threshold = first(number(p,"reorderThreshold"), old==null?null:old.reorderThreshold());
        String condition = f.condition()!=null?f.condition():old==null?null:old.condition();
        String actor = first(f.actorId(), text(p,"approvedBy"), text(p,"verifiedBy"), text(p,"issuedBy"), text(p,"receivedBy"), text(p,"returnedBy"), text(p,"issuedTo"), text(p,"assignedTo"), text(p,"changedBy"), old==null?null:old.actorId());
        projections.save(new ReportingProjection(id,f.entityType(),f.entityId(),status,dept,project,item,vendor,related,condition,qty,est,ordered,accepted,rejected,available,threshold,actor,f.producer(),old==null?f.occurredAt():old.createdAt(),Instant.now(),f.occurredAt(),f.eventId(),f.eventType()));
    }
    private static Long sumItemQuantities(JsonNode items) {
        if (items == null || !items.isArray()) return null;
        long total = 0L;
        for (JsonNode item : items) {
            JsonNode quantity = item.get("quantity");
            if (quantity == null || !quantity.isNumber()) {
                throw new IllegalArgumentException("PO_CREATED items[].quantity must be numeric");
            }
            total += Math.round(quantity.doubleValue());
        }
        return total;
    }
    private static Long sumItemValues(JsonNode items) {
        if (items == null || !items.isArray()) return null;
        long total = 0L;
        for (JsonNode item : items) {
            JsonNode quantity = item.get("quantity");
            JsonNode unitPrice = item.get("unitPrice");
            if (quantity == null || !quantity.isNumber() || unitPrice == null || !unitPrice.isNumber()) {
                throw new IllegalArgumentException("PO_CREATED items require numeric quantity and unitPrice");
            }
            total += Math.round(quantity.doubleValue() * unitPrice.doubleValue());
        }
        return total;
    }
    private static String text(JsonNode n,String k){JsonNode v=n==null?null:n.get(k);return v==null||v.isNull()?null:v.asText();}
    private static Long number(JsonNode n,String k){JsonNode v=n==null?null:n.get(k);return v==null||v.isNull()||!v.isNumber()?null:Math.round(v.doubleValue());}
    private static Instant instant(String s){try{if(s==null||s.isBlank())throw new IllegalArgumentException("occurredAt is required");return Instant.parse(s);}catch(Exception e){throw new IllegalArgumentException("occurredAt must be an ISO-8601 instant",e);}}
    private static Long first(Long a,Long b){return a!=null?a:b;}
    private static Long first(Long a,Long b,Long c){return a!=null?a:(b!=null?b:c);}
    private static String first(String... values){for(String v:values)if(v!=null&&!v.isBlank())return v;return null;}
}
