package com.recnaile.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.reporting.model.ReportingFact;
import com.recnaile.reporting.model.ReportingProjection;
import com.recnaile.reporting.repository.ReportingFactRepository;
import com.recnaile.reporting.repository.ReportingProjectionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.*;

class ReportingEventProcessorTest {
    private ReportingFactRepository facts;
    private ReportingProjectionRepository projections;
    private ReportingEventProcessor processor;

    @BeforeEach void setUp() {
        facts = mock(ReportingFactRepository.class);
        projections = mock(ReportingProjectionRepository.class);
        when(projections.findById(any())).thenReturn(Optional.empty());
        processor = new ReportingEventProcessor(new ObjectMapper(), facts, projections, new SimpleMeterRegistry());
    }

    @Test void indentSubmittedCreatesFactAndProjection() {
        processor.process("""
          {"eventId":"e-1","eventType":"INDENT_SUBMITTED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"indent-service","payload":{"indentId":"IND-1","departmentId":"D-1","projectId":"P-1","requestedBy":"U-1","estimatedValue":1500}}
          """);
        verify(facts).insert(ArgumentMatchers.<ReportingFact>argThat(f -> f.eventId().equals("e-1") && f.entityType().equals("INDENT") && f.entityId().equals("IND-1")));
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p -> p.id().equals("INDENT:IND-1") && p.status().equals("PENDING_VERIFICATION") && p.departmentId().equals("D-1")));
    }

    @Test void outwardIssuedStoresQuantityAsEventFact() {
        processor.process("""
          {"eventId":"e-2","eventType":"OUTWARD_ISSUED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"outward-service","payload":{"outwardId":"OUT-1","itemId":"ITEM-1","quantity":3,"issuedTo":"U-1","departmentId":"D-1","reusable":false}}
          """);
        verify(facts).insert(ArgumentMatchers.<ReportingFact>argThat(f -> f.entityType().equals("OUTWARD") && f.quantity().equals(3.0)));
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p -> p.entityType().equals("OUTWARD") && p.itemId().equals("ITEM-1")));
    }

    @Test void duplicateFactDoesNotCreateAnotherFactButProjectionRemainsReplaySafe() {
        doThrow(new DuplicateKeyException("duplicate event")).when(facts).insert(any(ReportingFact.class));
        processor.process("""
          {"eventId":"e-3","eventType":"INDENT_APPROVED","eventVersion":1,"occurredAt":"2026-10-12T10:00:00Z","producer":"indent-service","payload":{"indentId":"IND-2","approvedBy":"U-2","approverRole":"TL","estimatedValue":500}}
          """);
        verify(facts).insert(any(ReportingFact.class));
        verify(projections).save(ArgumentMatchers.<ReportingProjection>argThat(p -> p.status().equals("APPROVED")));
    }

    @Test void olderEventDoesNotOverwriteNewerProjection() {
        ReportingProjection current = new ReportingProjection("INDENT:IND-3","INDENT","IND-3","APPROVED",null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,Instant.parse("2026-10-01T00:00:00Z"),Instant.parse("2026-10-10T00:00:00Z"),Instant.parse("2026-10-10T00:00:00Z"),"e-new","INDENT_APPROVED");
        when(projections.findById("INDENT:IND-3")).thenReturn(Optional.of(current));
        processor.process("""
          {"eventId":"e-old","eventType":"INDENT_SUBMITTED","eventVersion":1,"occurredAt":"2026-10-01T00:00:00Z","producer":"indent-service","payload":{"indentId":"IND-3","departmentId":"D-1","projectId":"P-1","requestedBy":"U-1","estimatedValue":100}}
          """);
        verify(projections, never()).save(any(ReportingProjection.class));
    }
}
