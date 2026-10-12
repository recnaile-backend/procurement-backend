package com.recnaile.reporting.service;

import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import com.recnaile.reporting.repository.ReportingSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportingRecoveryServiceTest {
    @Test
    void rebuildDeletesOnlyDerivedProjectionsAndReplaysRetainedFacts() {
        ReportingFactRepository facts = mock(ReportingFactRepository.class);
        ReportingProjectionRepository projections = mock(ReportingProjectionRepository.class);
        ReportingSnapshotRepository snapshots = mock(ReportingSnapshotRepository.class);
        ReportingEventProcessor processor = mock(ReportingEventProcessor.class);
        KafkaListenerEndpointRegistry listeners = mock(KafkaListenerEndpointRegistry.class);
        ReportingFact fact = new ReportingFact("event-1", "INDENT_SUBMITTED", Instant.parse("2026-10-12T10:00:00Z"),
            "indent-service", "INDENT", "IND-1", null, "D-1", "P-1", null, null,
            "PENDING_VERIFICATION", null, null, null, null, null, null, null, null, null, null,
            null, null, Instant.parse("2026-10-12T10:01:00Z"));
        when(facts.count()).thenReturn(1L);
        when(projections.count()).thenReturn(1L);
        when(snapshots.count()).thenReturn(4L);
        when(facts.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(fact)));

        ReportingRecoveryService recovery = new ReportingRecoveryService(facts, projections, snapshots, processor, listeners);
        var result = recovery.rebuildProjections();

        verify(projections).deleteAll();
        verify(processor).rebuildProjectionFromFact(fact);
        verify(facts, never()).deleteAll();
        verify(snapshots, never()).deleteAll();
        assertEquals("COMPLETED", result.get("status"));
        assertEquals(1L, result.get("factsReplayed"));
        assertEquals(4L, result.get("snapshotsPreserved"));
    }
}
