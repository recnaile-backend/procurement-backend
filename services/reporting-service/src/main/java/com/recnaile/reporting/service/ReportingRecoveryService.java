package com.recnaile.reporting.service;

import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.model.ReportingSnapshot;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import com.recnaile.reporting.repository.ReportingSnapshotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Operational recovery tools for rebuilding the derived projection collection from retained event facts. */
@Service
public class ReportingRecoveryService {
    private static final int BATCH_SIZE = 500;
    private final ReportingFactRepository facts;
    private final ReportingProjectionRepository projections;
    private final ReportingSnapshotRepository snapshots;
    private final ReportingEventProcessor processor;
    private final KafkaListenerEndpointRegistry listeners;

    public ReportingRecoveryService(ReportingFactRepository facts,
                                    ReportingProjectionRepository projections,
                                    ReportingSnapshotRepository snapshots,
                                    ReportingEventProcessor processor,
                                    KafkaListenerEndpointRegistry listeners) {
        this.facts = facts;
        this.projections = projections;
        this.snapshots = snapshots;
        this.processor = processor;
        this.listeners = listeners;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", Instant.now());
        result.put("eventFactCount", facts.count());
        result.put("projectionCount", projections.count());
        result.put("snapshotCount", snapshots.count());
        result.put("rebuildAvailable", true);
        result.put("rebuildSource", "reporting_event_facts");
        result.put("snapshotPolicy", "Snapshots are retained and are not modified by projection rebuilds");
        return result;
    }

    /**
     * Recreates all derived projections from retained facts. The Kafka listener is stopped during
     * the rebuild so a live event cannot race with projection deletion/replay. If replay fails,
     * rerunning this operation is safe; facts remain untouched and projections are derived data.
     */
    public synchronized Map<String, Object> rebuildProjections() {
        MessageListenerContainer listener = listeners.getListenerContainer("reporting-events-listener");
        boolean restartListener = listener != null && listener.isRunning();
        Instant startedAt = Instant.now();
        long factCount = facts.count();
        long replayed = 0;
        if (restartListener) listener.stop();
        try {
            projections.deleteAll();
            int pageNumber = 0;
            Page<ReportingFact> page;
            do {
                page = facts.findAll(PageRequest.of(pageNumber, BATCH_SIZE,
                    Sort.by(Sort.Order.asc("occurredAt"), Sort.Order.asc("ingestedAt"), Sort.Order.asc("eventId"))));
                for (ReportingFact fact : page.getContent()) {
                    processor.rebuildProjectionFromFact(fact);
                    replayed++;
                }
                pageNumber++;
            } while (page.hasNext());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "COMPLETED");
            result.put("startedAt", startedAt);
            result.put("completedAt", Instant.now());
            result.put("eventFactsAtStart", factCount);
            result.put("factsReplayed", replayed);
            result.put("projectionsAfterRebuild", projections.count());
            result.put("snapshotsPreserved", snapshots.count());
            result.put("note", "Rebuilt from retained facts. Values not stored in reporting_event_facts cannot be reconstructed.");
            return result;
        } finally {
            if (restartListener && listener != null && !listener.isRunning()) listener.start();
        }
    }
}
