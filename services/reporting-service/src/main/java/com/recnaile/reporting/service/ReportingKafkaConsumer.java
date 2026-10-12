package com.recnaile.reporting.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportingKafkaConsumer {
    private final ReportingEventProcessor processor;
    private final MeterRegistry meters;
    public ReportingKafkaConsumer(ReportingEventProcessor processor, MeterRegistry meters) { this.processor=processor; this.meters=meters; }

    @KafkaListener(id = "reporting-events-listener", topicPattern = "${app.kafka.topic-pattern:.*(indent|purchase|po-|inward|goods|inventory|outward|return|asset|sla|audit).*}", groupId = "${spring.kafka.consumer.group-id:reporting-service}")
    public void consume(String message) { processor.process(message); }
}
