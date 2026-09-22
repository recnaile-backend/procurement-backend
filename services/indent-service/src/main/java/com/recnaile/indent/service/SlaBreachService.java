package com.recnaile.indent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.indent.config.ApprovalProperties;
import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import com.recnaile.indent.repository.IndentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaBreachService {

    private static final int BATCH_SIZE = 100;
    private static final String TOPIC = "procurement.indent.events";

    private final IndentRepository indentRepository;
    private final MongoTemplate mongoTemplate;
    private final ApprovalProperties approvalProperties;
    private final OutboxEventService outboxEventService;

    /**
     * Finds pending approvals whose SLA has expired and atomically claims each
     * indent before creating its SLA_BREACH outbox event.
     *
     * The claim prevents multiple scheduler instances from emitting duplicate
     * breach events for the same indent. The Mongo transaction keeps the claim
     * and outbox write in the same transaction, so a failed transaction rolls
     * the claim back.
     */
    @Scheduled(fixedDelayString = "${app.approval.sla-check-interval:60000}")
    @Transactional
    public void detectAndPublishBreaches() {
        Instant now = Instant.now();

        List<Indent> overdueIndents =
                indentRepository
                        .findByStatusAndApprovalDueAtLessThanEqualOrderByApprovalDueAtAsc(
                                IndentStatus.PENDING_APPROVAL,
                                now,
                                PageRequest.of(0, BATCH_SIZE));

        for (Indent candidate : overdueIndents) {
            publishIfClaimed(candidate, now);
        }
    }

    private void publishIfClaimed(Indent candidate, Instant now) {
        Query claimQuery = new Query(
                new Criteria().andOperator(
                        Criteria.where("_id").is(candidate.getId()),
                        Criteria.where("status").is(IndentStatus.PENDING_APPROVAL),
                        Criteria.where("approvalDueAt").lte(now),
                        new Criteria().orOperator(
                                Criteria.where("slaBreachAt").exists(false),
                                Criteria.where("slaBreachAt").is(null))));

        Update claimUpdate = new Update()
                .set("slaBreachAt", now)
                .set("updatedAt", now);

        Indent claimed =
                mongoTemplate.findAndModify(
                        claimQuery,
                        claimUpdate,
                        FindAndModifyOptions.options().returnNew(true),
                        Indent.class);

        if (claimed == null) {
            log.debug(
                    "SLA breach claim lost or already processed: indentId={}",
                    candidate.getId());
            return;
        }

        Duration overdue =
                Duration.between(claimed.getApprovalDueAt(), now);

        outboxEventService.create(
                "SLA_BREACH",
                "1",
                now,
                claimed.getId(),
                null,
                null,
                null,
                null,
                TOPIC,
                new SlaBreachEvent(
                        "INDENT",
                        claimed.getId(),
                        hours(approvalProperties.getSlaDuration()),
                        hours(overdue)));
    }

    private BigDecimal hours(Duration duration) {
        return BigDecimal.valueOf(duration.toMillis())
                .divide(
                        BigDecimal.valueOf(3_600_000L),
                        4,
                        RoundingMode.HALF_UP);
    }

    private record SlaBreachEvent(
            String entityType,
            String entityId,
            BigDecimal slaWindowHours,
            BigDecimal overdueSinceHours) {
    }
}
