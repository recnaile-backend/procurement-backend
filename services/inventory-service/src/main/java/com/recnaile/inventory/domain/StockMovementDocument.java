package com.recnaile.inventory.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("stock_movements")
public class StockMovementDocument {
    @Id private String id;
    private String eventId;
    private String eventType;
    private String sourceTransactionId;
    private String itemId;
    private String departmentId;
    private String projectId;
    private String location;
    private StockType stockType;
    private double quantity;
    private double delta;
    private String reason;
    private Instant occurredAt;
    private Instant createdAt;
    private String correlationId;
    private String traceId;
    private String actorId;

    public StockMovementDocument() {}

    public StockMovementDocument(String id, String eventId, String eventType,
            String sourceTransactionId, String itemId, String departmentId,
            String projectId, String location, StockType stockType, double quantity,
            double delta, String reason, Instant occurredAt, Instant createdAt,
            String correlationId, String traceId, String actorId) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.sourceTransactionId = sourceTransactionId;
        this.itemId = itemId;
        this.departmentId = departmentId;
        this.projectId = projectId;
        this.location = location;
        this.stockType = stockType;
        this.quantity = quantity;
        this.delta = delta;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.correlationId = correlationId;
        this.traceId = traceId;
        this.actorId = actorId;
    }

    public StockMovementDocument(String eventId, String eventType, String sourceTransactionId,
            String itemId, String departmentId, String projectId, String location,
            StockType stockType, double quantity, double delta, String reason,
            Instant occurredAt, Instant createdAt, String correlationId, String traceId, String actorId) {
        this.eventId=eventId; this.eventType=eventType; this.sourceTransactionId=sourceTransactionId;
        this.itemId=itemId; this.departmentId=departmentId; this.projectId=projectId; this.location=location;
        this.stockType=stockType; this.quantity=quantity; this.delta=delta; this.reason=reason;
        this.occurredAt=occurredAt; this.createdAt=createdAt; this.correlationId=correlationId;
        this.traceId=traceId; this.actorId=actorId;
    }
    public String getId(){return id;} public String getEventId(){return eventId;}
    public String getEventType(){return eventType;} public String getSourceTransactionId(){return sourceTransactionId;}
    public String getItemId(){return itemId;} public String getDepartmentId(){return departmentId;}
    public String getProjectId(){return projectId;} public String getLocation(){return location;}
    public StockType getStockType(){return stockType;} public double getQuantity(){return quantity;}
    public double getDelta(){return delta;} public String getReason(){return reason;}
    public Instant getOccurredAt(){return occurredAt;} public Instant getCreatedAt(){return createdAt;}
    public String getCorrelationId(){return correlationId;} public String getTraceId(){return traceId;}
    public String getActorId(){return actorId;}
}
