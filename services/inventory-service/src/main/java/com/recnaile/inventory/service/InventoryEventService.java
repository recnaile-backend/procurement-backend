package com.recnaile.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.repository.*;
import com.recnaile.inventory.exception.InsufficientStockException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryEventService {
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final InventoryAuditRepository auditRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public InventoryEventService(StockBalanceRepository balanceRepository, StockMovementRepository movementRepository,
            IdempotencyRepository idempotencyRepository, OutboxRepository outboxRepository,
            InventoryAuditRepository auditRepository, MongoTemplate mongoTemplate, ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.balanceRepository=balanceRepository; this.movementRepository=movementRepository;
        this.idempotencyRepository=idempotencyRepository; this.outboxRepository=outboxRepository;
        this.auditRepository=auditRepository; this.mongoTemplate=mongoTemplate;
        this.objectMapper=objectMapper; this.meterRegistry=meterRegistry;
    }

    @Transactional
    public void consume(JsonNode event) {
        String eventId=text(event,"eventId");
        String eventType=text(event,"eventType");
        if (eventId == null || eventType == null) throw new IllegalArgumentException("Event envelope requires eventId and eventType");
        if (idempotencyRepository.existsByEventId(eventId)) {
            meterRegistry.counter("inventory.events.duplicate", "eventType", eventType).increment();
            return;
        }
        switch (eventType) {
            case "GOODS_RECEIVED" -> goodsReceived(event);
            case "OUTWARD_ISSUED" -> outwardIssued(event);
            case "RETURN_RECORDED" -> returnRecorded(event);
            default -> meterRegistry.counter("inventory.events.ignored", "eventType", eventType).increment();
        }
        idempotencyRepository.save(new IdempotencyDocument(eventId,eventType,Instant.now()));
    }

    private void goodsReceived(JsonNode e) {
        JsonNode p=e.path("payload");
        String itemId=required(p,"itemId"), sourceId=required(p,"grnId");
        int accepted=number(p,"acceptedQuantity"), rejected=number(p,"rejectedQuantity");
        if (accepted < 0 || rejected < 0) throw new IllegalArgumentException("Received quantities cannot be negative");
        if (accepted > 0) apply(e,itemId,sourceId,StockType.AVAILABLE,accepted,"GOODS_RECEIVED");
        if (rejected > 0) apply(e,itemId,sourceId,StockType.DEFECT,rejected,"GOODS_RECEIVED_REJECTED");
    }

    private void outwardIssued(JsonNode e) {
        JsonNode p=e.path("payload");
        String itemId=required(p,"itemId"), sourceId=required(p,"outwardId");
        int qty=number(p,"quantity");
        if (qty <= 0) throw new IllegalArgumentException("Issued quantity must be greater than zero");
        apply(e,itemId,sourceId,StockType.AVAILABLE,-qty,"OUTWARD_ISSUED");
    }

    private void returnRecorded(JsonNode e) {
        JsonNode p=e.path("payload");
        String itemId=required(p,"itemId"), sourceId=required(p,"returnId");
        int qty=number(p,"quantity");
        if (qty <= 0) throw new IllegalArgumentException("Return quantity must be greater than zero");
        String condition=required(p,"condition");
        apply(e,itemId,sourceId,"GOOD".equals(condition)?StockType.AVAILABLE:StockType.DEFECT,qty,"RETURN_"+condition);
    }

    private void apply(JsonNode event,String itemId,String sourceId,StockType type,int delta,String reason) {
        String departmentName=text(event.path("payload"),"departmentName");
        if (departmentName == null || departmentName.isBlank()) {
            departmentName=text(event.path("payload"),"departmentId");
        }
        String projectId=text(event.path("payload"),"projectId");
        String location=text(event.path("payload"),"location");
        StockBalanceDocument b=getOrCreate(itemId,departmentName,projectId,location);
        int beforeAvailable=b.getAvailableQuantity(), beforeDefect=b.getDefectQuantity();
        int newAvailable=beforeAvailable, newDefect=beforeDefect;
        if(type==StockType.AVAILABLE) newAvailable=beforeAvailable+delta; else newDefect=beforeDefect+delta;
        if(newAvailable < 0) {
            meterRegistry.counter("inventory.stock.rejected", "reason", "INSUFFICIENT_AVAILABLE").increment();
            throw new InsufficientStockException(itemId, Math.abs(delta), beforeAvailable);
        }
        boolean wasLow=b.isLowStock();
        b.setAvailableQuantity(newAvailable); b.setDefectQuantity(newDefect);
        recalculateStatus(b);
        b.setUpdatedAt(Instant.now());
        try { balanceRepository.save(b); }
        catch (OptimisticLockingFailureException ex) {
            meterRegistry.counter("inventory.stock.concurrency_conflict").increment();
            throw ex;
        }

        String eventId=text(event,"eventId");
        String movementId=eventId+":"+type+":"+UUID.randomUUID();
        StockMovementDocument movement=new StockMovementDocument(movementId,eventId,text(event,"eventType"),sourceId,itemId,
            departmentName,projectId,location,type,Math.abs(delta),delta,reason,
            instant(event,"occurredAt"),Instant.now(),text(event,"correlationId"),text(event,"traceId"),
            text(event.path("payload"),"receivedBy"));
        movementRepository.save(movement);

        Map<String,Object> before=Map.of("availableQuantity",beforeAvailable,"defectQuantity",beforeDefect,"status",statusName(beforeAvailable,b.getMinimumStock()));
        Map<String,Object> after=Map.of("availableQuantity",newAvailable,"defectQuantity",newDefect,"status",b.getStatus().name());
        auditRepository.save(new InventoryAuditDocument("STOCK_BALANCE",b.getBalanceKey(),"STOCK_CHANGED",eventId,
            text(event.path("payload"),"receivedBy"),null,Instant.now(),text(event,"correlationId"),text(event,"traceId"),before,after));
        meterRegistry.counter("inventory.stock.movements","type",type.name()).increment();
        meterRegistry.gauge("inventory.available.quantity", List.of(io.micrometer.core.instrument.Tag.of("itemId",itemId)), b.getAvailableQuantity());

        if(!wasLow && b.isLowStock()) {
            Map<String,Object> payload=Map.of("itemId",itemId,"availableQuantity",newAvailable,"reorderThreshold",b.getMinimumStock());
            outboxRepository.save(new OutboxEventDocument(UUID.randomUUID().toString(),"LOW_STOCK_ALERT",1,Instant.now(),
                "inventory-service",text(event,"correlationId"),text(event,"traceId"),payload));
            meterRegistry.counter("inventory.low_stock.alerts").increment();
        }
    }

    private StockBalanceDocument getOrCreate(String itemId,String departmentName,String project,String location) {
        String d=blankToNull(departmentName), p=blankToNull(project), l=blankToNull(location);
        String key=key(itemId,d,p,l);
        return balanceRepository.findByBalanceKey(key).orElseGet(() -> {
            StockBalanceDocument b=new StockBalanceDocument();
            b.setBalanceKey(key); b.setItemId(itemId); b.setDepartmentName(d); b.setProjectId(p); b.setLocation(l);
            b.setAvailableQuantity(0); b.setDefectQuantity(0); b.setMinimumStock(0); b.setMaximumStock(0);
            b.setLowStock(false); b.setStatus(StockStatus.OUT_OF_STOCK); b.setUpdatedAt(Instant.now());
            return b;
        });
    }

    public static String key(String itemId,String dept,String project,String location) {
        return String.join("|", itemId, nullToBlank(dept), nullToBlank(project), nullToBlank(location));
    }
    private void recalculateStatus(StockBalanceDocument b) {
        boolean low=b.getAvailableQuantity() > 0 && b.getAvailableQuantity() <= b.getMinimumStock();
        b.setLowStock(low);
        b.setStatus(b.getAvailableQuantity() <= 0 ? StockStatus.OUT_OF_STOCK : low ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK);
    }
    private String statusName(int available,int min){return available<=0?StockStatus.OUT_OF_STOCK.name():(available<=min?StockStatus.LOW_STOCK.name():StockStatus.IN_STOCK.name());}
    private static String text(JsonNode n,String f){JsonNode v=n==null?null:n.get(f);return v==null||v.isNull()?null:v.asText();}
    private static String required(JsonNode n,String f){String v=text(n,f);if(v==null||v.isBlank())throw new IllegalArgumentException("Missing payload field: "+f);return v;}
    private static int number(JsonNode n,String f){JsonNode v=n.get(f);if(v==null||!v.isIntegralNumber())throw new IllegalArgumentException("Missing/invalid integer payload field: "+f);return v.asInt();}
    private static Instant instant(JsonNode n,String f){String v=text(n,f);return v==null?Instant.now():Instant.parse(v);}
    private static String blankToNull(String v){return v==null||v.isBlank()?null:v;}
    private static String nullToBlank(String v){return v==null?"":v;}
}
