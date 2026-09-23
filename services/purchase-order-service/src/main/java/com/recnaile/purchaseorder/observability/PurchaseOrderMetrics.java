package com.recnaile.purchaseorder.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderMetrics {

    private final MeterRegistry meterRegistry;

    public PurchaseOrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCreated() {
        Counter.builder("purchase_order.created")
                .description("Purchase orders successfully created")
                .register(meterRegistry)
                .increment();
    }

    public void recordLifecycleTransition(String status) {
        Counter.builder("purchase_order.lifecycle.transitions")
                .description("Purchase order lifecycle transitions")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    public void recordOutboxPublished(String eventType) {
        Counter.builder("purchase_order.outbox.published")
                .description("Purchase order outbox events successfully published")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }

    public void recordOutboxFailure(String eventType) {
        Counter.builder("purchase_order.outbox.failures")
                .description("Purchase order outbox publication failures")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }
}
