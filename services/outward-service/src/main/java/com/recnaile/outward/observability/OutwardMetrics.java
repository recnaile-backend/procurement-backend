package com.recnaile.outward.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutwardMetrics {
    private final MeterRegistry registry;

    public OutwardMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void created() { counter("outward_commands_created").increment(); }
    public void approved() { counter("outward_commands_approved").increment(); }
    public void rejected() { counter("outward_commands_rejected").increment(); }
    public void issued() { counter("outward_issue_transactions_created").increment(); }
    public void cancelled() { counter("outward_commands_cancelled").increment(); }
    public void idempotentReplay() { counter("outward_idempotency_replays").increment(); }
    public void stockRejected() { counter("outward_stock_rejections").increment(); }
    public void outboxCreated() { counter("outward_outbox_created").increment(); }
    public void outboxPublished() { counter("outward_outbox_published").increment(); }
    public void outboxFailed() { counter("outward_outbox_failed").increment(); }

    private Counter counter(String name) {
        return Counter.builder(name).register(registry);
    }
}
