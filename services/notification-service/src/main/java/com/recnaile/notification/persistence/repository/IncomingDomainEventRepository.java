package com.recnaile.notification.persistence.repository;

import com.recnaile.notification.domain.model.IncomingDomainEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IncomingDomainEventRepository extends MongoRepository<IncomingDomainEvent, String> {
    Optional<IncomingDomainEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
