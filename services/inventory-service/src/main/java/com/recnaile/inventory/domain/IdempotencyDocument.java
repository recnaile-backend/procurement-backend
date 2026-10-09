package com.recnaile.inventory.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document("processed_events")
public class IdempotencyDocument {
    @Id private String id;
    @Indexed(unique = true) private String eventId;
    private String eventType;
    private Instant processedAt;
    public IdempotencyDocument() {}
    public IdempotencyDocument(String eventId,String eventType,Instant processedAt){this.eventId=eventId;this.eventType=eventType;this.processedAt=processedAt;}
    public String getId(){return id;} public String getEventId(){return eventId;} public String getEventType(){return eventType;} public Instant getProcessedAt(){return processedAt;}
}
