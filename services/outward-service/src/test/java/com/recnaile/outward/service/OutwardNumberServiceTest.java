package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.recnaile.outward.repository.OutwardSequenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OutwardNumberServiceTest {

    @Test
    void allocatesEncodedNumberForIndianFiscalYear() {
        AtomicLong sequence = new AtomicLong();
        OutwardSequenceRepository repository = new OutwardSequenceRepository(null) {
            @Override
            public long nextSequence(String fiscalYearKey) {
                assertEquals("OUTWARD:2026-27", fiscalYearKey);
                return sequence.incrementAndGet();
            }
        };
        Clock clock = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"), ZoneId.of("Asia/Kolkata"));
        OutwardNumberService service = new OutwardNumberService(repository, clock);

        assertEquals("OUT%2F26-27%2F001", service.nextNumber());
        assertEquals("OUT%2F26-27%2F002", service.nextNumber());
    }

    @Test
    void fiscalYearStartsInApril() {
        assertEquals(2026, OutwardNumberService.fiscalYearStart(java.time.LocalDate.of(2026, 4, 1)));
        assertEquals(2025, OutwardNumberService.fiscalYearStart(java.time.LocalDate.of(2026, 3, 31)));
    }
}
