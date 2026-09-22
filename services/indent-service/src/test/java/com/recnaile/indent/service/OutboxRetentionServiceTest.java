package com.recnaile.indent.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.recnaile.indent.domain.model.OutboxEvent.OutboxStatus;
import com.recnaile.indent.repository.OutboxEventRepository;

class OutboxRetentionServiceTest {

    @Test
    void purgesOnlyPublishedEventsOlderThanRetention() {

        OutboxEventRepository repository =
                org.mockito.Mockito.mock(
                        OutboxEventRepository.class);

        when(repository.deleteByStatusAndPublishedAtBefore(
                eq(OutboxStatus.PUBLISHED),
                any(Instant.class)))
                .thenReturn(3L);

        OutboxRetentionService service =
                new OutboxRetentionService(repository);

        ReflectionTestUtils.setField(
                service,
                "publishedRetention",
                Duration.ofDays(7));

        Instant before = Instant.now();

        service.purgePublishedEvents();

        Instant after = Instant.now();

        ArgumentCaptor<Instant> cutoffCaptor =
                ArgumentCaptor.forClass(Instant.class);

        verify(repository).deleteByStatusAndPublishedAtBefore(
                eq(OutboxStatus.PUBLISHED),
                cutoffCaptor.capture());

        Instant capturedCutoff =
                cutoffCaptor.getValue();

        Instant earliestExpectedCutoff =
                before.minus(Duration.ofDays(7));

        Instant latestExpectedCutoff =
                after.minus(Duration.ofDays(7));

        /*
         * The service calculates:
         *
         * Instant.now() - 7 days
         *
         * Because the service's Instant.now() happens between
         * 'before' and 'after', the calculated cutoff must also
         * fall between these two expected boundaries.
         */
        assertTrue(
                !capturedCutoff.isBefore(earliestExpectedCutoff),
                "Cutoff must not be earlier than before - retention");

        assertTrue(
                !capturedCutoff.isAfter(latestExpectedCutoff),
                "Cutoff must not be later than after - retention");
    }
}