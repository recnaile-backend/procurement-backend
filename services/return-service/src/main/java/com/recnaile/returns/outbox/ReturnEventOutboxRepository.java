package com.recnaile.returns.outbox;

import com.recnaile.returns.model.enums.OutboxStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReturnEventOutboxRepository extends MongoRepository<ReturnEventOutbox, String> {
    List<ReturnEventOutbox> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
