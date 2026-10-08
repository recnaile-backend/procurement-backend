package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.OutboxEventDocument;
import com.recnaile.inventory.repository.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String,Object> kafka;
    private final String topic;
    private final long claimLeaseMs;

    public OutboxPublisher(
            OutboxRepository repository,
            MongoTemplate mongoTemplate,
            KafkaTemplate<String,Object> kafka,
            @Value("${app.kafka.outbox-topic:procurement.events}") String topic,
            @Value("${app.kafka.outbox-claim-lease-ms:30000}") long claimLeaseMs) {
        this.repository=repository;this.mongoTemplate=mongoTemplate;this.kafka=kafka;this.topic=topic;this.claimLeaseMs=claimLeaseMs;
    }

    @Scheduled(fixedDelayString="${app.kafka.outbox-publish-delay-ms:1000}")
    public void publish() {
        List<OutboxEventDocument> pending=repository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEventDocument candidate:pending) {
            publishOne(candidate.getId());
        }
    }

    private void publishOne(String id) {
        String token=UUID.randomUUID().toString();
        Instant now=Instant.now();
        Instant staleBefore=now.minus(Duration.ofMillis(claimLeaseMs));
        Query claimQuery=new Query(new Criteria().andOperator(
                Criteria.where("_id").is(id),
                Criteria.where("published").is(false),
                new Criteria().orOperator(
                        Criteria.where("processing").is(false),
                        Criteria.where("claimedAt").lt(staleBefore),
                        Criteria.where("claimedAt").exists(false))));
        Update claimUpdate=new Update().set("processing",true).set("claimToken",token).set("claimedAt",now);
        OutboxEventDocument event=mongoTemplate.findAndModify(claimQuery,claimUpdate,
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),
                OutboxEventDocument.class);
        if(event==null) return;

        try {
            kafka.send(topic,event.getEventId(),event).get();
            Query completeQuery=new Query(new Criteria().andOperator(
                    Criteria.where("_id").is(event.getId()),
                    Criteria.where("claimToken").is(token),
                    Criteria.where("published").is(false)));
            mongoTemplate.updateFirst(completeQuery,
                    new Update().set("published",true).set("processing",false)
                            .unset("claimToken").unset("claimedAt").set("publishedAt",Instant.now()),
                    OutboxEventDocument.class);
        } catch(Exception ex) {
            Query releaseQuery=new Query(new Criteria().andOperator(
                    Criteria.where("_id").is(event.getId()),
                    Criteria.where("claimToken").is(token)));
            mongoTemplate.updateFirst(releaseQuery,
                    new Update().set("processing",false).unset("claimToken").unset("claimedAt")
                            .inc("attempts",1), OutboxEventDocument.class);
        }
    }
}
