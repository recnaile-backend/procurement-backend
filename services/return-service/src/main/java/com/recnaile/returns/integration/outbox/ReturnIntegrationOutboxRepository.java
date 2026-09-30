package com.recnaile.returns.integration.outbox;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReturnIntegrationOutboxRepository extends MongoRepository<ReturnIntegrationOutbox,String> {
    List<ReturnIntegrationOutbox> findTop50ByStatusOrderByCreatedAtAsc(IntegrationOutboxStatus status);
}
