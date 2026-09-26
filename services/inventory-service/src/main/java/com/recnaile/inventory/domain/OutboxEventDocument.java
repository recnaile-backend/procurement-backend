package com.recnaile.inventory.domain;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("outbox_events")
public class OutboxEventDocument {
    @Id private String id;
    private String eventId; private String eventType; private int eventVersion;
    private Instant occurredAt; private String producer; private String correlationId; private String traceId;
    private Map<String,Object> payload; private boolean published; private int attempts; private Instant createdAt; private Instant publishedAt;
    public OutboxEventDocument() {}
    public OutboxEventDocument(String eventId,String eventType,int eventVersion,Instant occurredAt,String producer,
        String correlationId,String traceId,Map<String,Object> payload){
        this.eventId=eventId;this.eventType=eventType;this.eventVersion=eventVersion;this.occurredAt=occurredAt;
        this.producer=producer;this.correlationId=correlationId;this.traceId=traceId;this.payload=payload;
        this.createdAt=Instant.now();this.published=false;this.attempts=0;
    }
    public String getId(){return id;} public String getEventId(){return eventId;} public String getEventType(){return eventType;}
    public int getEventVersion(){return eventVersion;} public Instant getOccurredAt(){return occurredAt;}
    public String getProducer(){return producer;} public String getCorrelationId(){return correlationId;}
    public String getTraceId(){return traceId;} public Map<String,Object> getPayload(){return payload;}
    public boolean isPublished(){return published;} public int getAttempts(){return attempts;} public Instant getCreatedAt(){return createdAt;}
    public Instant getPublishedAt(){return publishedAt;} public void markPublished(){published=true;publishedAt=Instant.now();}
    public void incrementAttempts(){attempts++;}
}
