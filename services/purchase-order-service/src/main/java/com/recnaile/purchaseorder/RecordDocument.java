package com.recnaile.purchaseorder;

import org.springframework.data.annotation.Id;
import java.time.Instant;
import java.util.Map;

public record RecordDocument(
    @Id String id,
    String referenceId,
    String name,
    String status,
    Map<String,Object> payload,
    Instant createdAt
) {}
