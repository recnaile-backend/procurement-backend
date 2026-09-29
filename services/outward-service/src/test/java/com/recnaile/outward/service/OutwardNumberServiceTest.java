package com.recnaile.outward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.recnaile.outward.repository.OutwardRequestRepository;
import com.recnaile.outward.repository.OutwardSequenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class OutwardNumberServiceTest {

    @Test
    void allocatesEncodedNumberForIndianFiscalYear() {
        OutwardSequenceRepository sequenceRepository = org.mockito.Mockito.mock(OutwardSequenceRepository.class);
        OutwardRequestRepository requestRepository = org.mockito.Mockito.mock(OutwardRequestRepository.class);
        when(sequenceRepository.nextSequence("OUTWARD:2026-27")).thenReturn(1L, 2L);
        when(requestRepository.existsByOutwardNumber("OUT%2F26-27%2F001")).thenReturn(false);
        when(requestRepository.existsByOutwardNumber("OUT%2F26-27%2F002")).thenReturn(false);

        Clock clock = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"), ZoneId.of("Asia/Kolkata"));
        OutwardNumberService service = new OutwardNumberService(sequenceRepository, requestRepository, clock);

        assertEquals("OUT%2F26-27%2F001", service.nextNumber());
        assertEquals("OUT%2F26-27%2F002", service.nextNumber());
    }

    @Test
    void skipsPersistedNumberWhenSequenceIsBehind() {
        OutwardSequenceRepository sequenceRepository = org.mockito.Mockito.mock(OutwardSequenceRepository.class);
        OutwardRequestRepository requestRepository = org.mockito.Mockito.mock(OutwardRequestRepository.class);
        when(sequenceRepository.nextSequence("OUTWARD:2026-27")).thenReturn(1L, 2L);
        when(requestRepository.existsByOutwardNumber("OUT%2F26-27%2F001")).thenReturn(true);
        when(requestRepository.existsByOutwardNumber("OUT%2F26-27%2F002")).thenReturn(false);

        Clock clock = Clock.fixed(Instant.parse("2026-09-26T04:00:00Z"), ZoneId.of("Asia/Kolkata"));
        OutwardNumberService service = new OutwardNumberService(sequenceRepository, requestRepository, clock);

        assertEquals("OUT%2F26-27%2F002", service.nextNumber());
        verify(sequenceRepository, times(2)).nextSequence("OUTWARD:2026-27");
    }

    @Test
    void fiscalYearStartsInApril() {
        assertEquals(2026, OutwardNumberService.fiscalYearStart(java.time.LocalDate.of(2026, 4, 1)));
        assertEquals(2025, OutwardNumberService.fiscalYearStart(java.time.LocalDate.of(2026, 3, 31)));
    }
}
