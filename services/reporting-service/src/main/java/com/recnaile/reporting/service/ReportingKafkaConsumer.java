package com.recnaile.reporting.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportingKafkaConsumer {
    private final ReportingEventProcessor processor;
    private final MeterRegistry meters;
    public ReportingKafkaConsumer(ReportingEventProcessor processor, MeterRegistry meters) { this.processor=processor; this.meters=meters; }

    @KafkaListener(id = "reporting-events-listener", topicPattern = "${app.kafka.topic-pattern:.*(indent|purchase|po-|inward|goods|inventory|outward|return|asset|sla|audit).*}", groupId = "${spring.kafka.consumer.group-id:reporting-service}")
    public void consume(String message) {
        meters.counter("reporting.kafka.messages.received").increment();
        Timer.Sample sample = Timer.start(meters);
        String outcome = "success";
        try {
            processor.process(message);
        } catch (RuntimeException exception) {
            outcome = "failure";
            meters.counter("reporting.kafka.messages.processing_failed").increment();
            throw exception;
        } finally {
            sample.stop(Timer.builder("reporting.kafka.consume")
                    .description("Time spent processing a reporting Kafka message")
                    .tag("outcome", outcome).register(meters));
        }
    }
}
