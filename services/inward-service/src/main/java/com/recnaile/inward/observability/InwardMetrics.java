package com.recnaile.inward.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Domain metrics exposed through the Actuator Prometheus endpoint. */
@Component
public class InwardMetrics {

    private final Counter grnCreated;
    private final Counter outboxCreated;
    private final Counter outboxPublished;
    private final Counter outboxFailed;
    private final Counter auditEventsCreated;

    public InwardMetrics(MeterRegistry registry) {
        this.grnCreated = Counter.builder("inward_grn_created")
                .description("GRNs successfully persisted")
                .register(registry);
        this.outboxCreated = Counter.builder("inward_outbox_events_created")
                .description("Transactional outbox events created")
                .register(registry);
        this.outboxPublished = Counter.builder("inward_outbox_events_published")
                .description("Transactional outbox events successfully published")
                .register(registry);
        this.outboxFailed = Counter.builder("inward_outbox_events_failed")
                .description("Transactional outbox publish attempts that failed")
                .register(registry);
        this.auditEventsCreated = Counter.builder("inward_audit_events_created")
                .description("Audit events added to the transactional outbox")
                .register(registry);
    }

    public void grnCreated() { grnCreated.increment(); }
    public void outboxCreated() { outboxCreated.increment(); }
    public void outboxPublished() { outboxPublished.increment(); }
    public void outboxFailed() { outboxFailed.increment(); }
    public void auditEventCreated() { auditEventsCreated.increment(); }
}
