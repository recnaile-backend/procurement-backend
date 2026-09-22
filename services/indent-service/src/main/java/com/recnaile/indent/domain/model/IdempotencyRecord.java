package com.recnaile.indent.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "indent_idempotency")
@CompoundIndex(
        name = "uk_idempotency_actor_operation_key",
        def = "{'actorId': 1, 'operation': 1, 'key': 1}",
        unique = true
)
public class IdempotencyRecord {

    @Id
    private String id;

    private String key;

    private String actorId;

    private String operation;

    private String resourceId;

    private String requestHash;

    private Integer responseStatus;

    private String responseBody;

    private Instant createdAt;

    private Instant expiresAt;
}