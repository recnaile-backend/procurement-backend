package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.OutboxEventDocument;
import com.recnaile.inventory.repository.OutboxRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final KafkaTemplate<String,Object> kafka;
    private final String topic;
    public OutboxPublisher(OutboxRepository repository,KafkaTemplate<String,Object> kafka,
        @Value("${app.kafka.outbox-topic:procurement.events}") String topic){this.repository=repository;this.kafka=kafka;this.topic=topic;}

    @Scheduled(fixedDelayString="${app.kafka.outbox-publish-delay-ms:1000}")
    public void publish() {
        List<OutboxEventDocument> pending=repository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for(OutboxEventDocument e:pending) {
            try {
                kafka.send(topic,e.getEventId(),e).get();
                e.markPublished(); repository.save(e);
            } catch(Exception ex) {
                e.incrementAttempts(); repository.save(e);
            }
        }
    }
}
