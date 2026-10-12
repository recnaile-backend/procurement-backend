package com.recnaile.reporting.repository;

import com.recnaile.reporting.model.ReportingFact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface ReportingFactRepository extends MongoRepository<ReportingFact, String> {
    Page<ReportingFact> findByEventTypeAndOccurredAtBetween(String eventType, Instant from, Instant to, Pageable pageable);
    Page<ReportingFact> findByOccurredAtBetween(Instant from, Instant to, Pageable pageable);
    List<ReportingFact> findByOccurredAtBetween(Instant from, Instant to);
    long countByEventType(String eventType);
    java.util.Optional<ReportingFact> findFirstByOrderByIngestedAtDesc();
    java.util.Optional<ReportingFact> findFirstByOrderByOccurredAtDesc();
}
