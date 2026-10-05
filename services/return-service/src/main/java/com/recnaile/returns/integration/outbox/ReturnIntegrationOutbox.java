package com.recnaile.returns.integration.outbox;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "return_integration_outbox")
@CompoundIndex(name = "integration_status_created_idx", def = "{'status': 1, 'createdAt': 1}")
public class ReturnIntegrationOutbox {
    @Id private String id;
    @Indexed(unique = true) private String eventId;
    private String aggregateId;
    private String eventType;
    private String target;
    private String topic;
    private String payload;
    private IntegrationOutboxStatus status;
    private int attempts;
    private Instant createdAt;
    private Instant publishedAt;
    private String lastError;
    @Version private Long version;

    protected ReturnIntegrationOutbox() {}

    public ReturnIntegrationOutbox(String eventId, String aggregateId, String eventType, String target, String topic, String payload, Instant createdAt) {
        this.eventId=eventId; this.aggregateId=aggregateId; this.eventType=eventType; this.target=target; this.topic=topic; this.payload=payload;
        this.createdAt=createdAt; this.status=IntegrationOutboxStatus.PENDING;
    }
    public String getId(){return id;} public String getEventId(){return eventId;} public String getAggregateId(){return aggregateId;}
    public String getEventType(){return eventType;} public String getTarget(){return target;} public String getTopic(){return topic;} public String getPayload(){return payload;}
    public IntegrationOutboxStatus getStatus(){return status;} public int getAttempts(){return attempts;} public Instant getCreatedAt(){return createdAt;}
    public Instant getPublishedAt(){return publishedAt;} public String getLastError(){return lastError;} public Long getVersion(){return version;}
    public void markSent(Instant at){status=IntegrationOutboxStatus.SENT; publishedAt=at; lastError=null;}
    public void markAttemptFailed(String error){attempts++; lastError=error; status=IntegrationOutboxStatus.PENDING;}
}
