package com.recnaile.returns.service;

import com.recnaile.returns.repository.ReturnRequestRepository;
import com.recnaile.returns.repository.ReturnSequenceRepository;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReturnNumberServiceTest {
    @Test void allocatesUppercaseEncodedNumber(){
        ReturnSequenceRepository seq=mock(ReturnSequenceRepository.class); ReturnRequestRepository repo=mock(ReturnRequestRepository.class);
        when(seq.nextSequence("RETURN:2026-27")).thenReturn(1L); when(repo.existsByReturnId("RETURN%2F26-27%2F001")).thenReturn(false);
        Clock clock=Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneId.of("Asia/Kolkata"));
        assertEquals("RETURN%2F26-27%2F001", new ReturnNumberService(seq,repo,clock).nextNumber());
    }
    @Test void fiscalYearStartsInApril(){
        assertEquals(2026, ReturnNumberService.fiscalYearStart(java.time.LocalDate.of(2026,4,1)));
        assertEquals(2025, ReturnNumberService.fiscalYearStart(java.time.LocalDate.of(2026,3,31)));
    }
}
