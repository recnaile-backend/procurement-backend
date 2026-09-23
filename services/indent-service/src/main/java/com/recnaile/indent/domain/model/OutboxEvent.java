package com.recnaile.indent.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "indent_outbox_events")
@CompoundIndex(
        name = "idx_outbox_status_created_at",
        def = "{'status': 1, 'createdAt': 1}"
)
@CompoundIndex(
        name = "idx_outbox_status_next_attempt_created_at",
        def = "{'status': 1, 'nextAttemptAt': 1, 'createdAt': 1}"
)
@CompoundIndex(
        name = "idx_outbox_event_type_created_at",
        def = "{'eventType': 1, 'createdAt': 1}"
)
public class OutboxEvent {

    @Id
    private String id;

    /**
     * Globally unique event identifier.
     */
    @Indexed(unique = true)
    private String eventId;

    /**
     * Kafka event type.
     * Example: INDENT_SUBMITTED
     */
    private String eventType;

    /**
     * Version of the event contract.
     */
    private String eventVersion;

    /**
     * Time at which the business event occurred.
     */
    private Instant occurredAt;

    /**
     * Service that produced the event.
     */
    private String producerService;

    /**
     * Aggregate type.
     * Example: INDENT
     */
    private String aggregateType;

    /**
     * ID of the aggregate that produced the event.
     */
    private String aggregateId;

    /**
     * Correlation ID propagated through the request/event chain.
     */
    private String correlationId;

    /**
     * OpenTelemetry trace identifier for the originating request.
     */
    private String traceId;

    /**
     * ID of the event/command that caused this event, when available.
     */
    private String causationId;

    /**
     * Authenticated user who caused the business operation.
     */
    private String actorId;

    /**
     * Role of the actor, when applicable.
     */
    private String actorRole;

    /**
     * Kafka topic to which this event should be published.
     */
    private String topic;

    /**
     * Serialized event payload.
     *
     * The payload is transport data. Core indent business data remains
     * strongly typed inside the domain model.
     */
    private String payload;

    /**
     * Current delivery state.
     */
    private OutboxStatus status;

    /**
     * Number of publishing attempts.
     */
    private int attemptCount;

    /**
     * Last publishing attempt time.
     */
    private Instant lastAttemptAt;

    /**
     * Next time this event becomes eligible for another publish attempt.
     *
     * Null means the event is immediately eligible (for example, a newly
     * created PENDING event).
     */
    private Instant nextAttemptAt;

    /**
     * Last publishing error, if any.
     *
     * Do not store secrets or access tokens here.
     */
    private String lastError;

    /**
     * Successful publication time.
     */
    private Instant publishedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private long version;

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}