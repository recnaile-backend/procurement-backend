package com.recnaile.outward.service;

import com.recnaile.outward.domain.number.OutwardNumberGenerator;
import com.recnaile.outward.repository.OutwardRequestRepository;
import com.recnaile.outward.repository.OutwardSequenceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Allocates outward numbers in the same backend-owned pattern used by the
 * Inward/GRN service: one atomic sequence per Indian financial year, with the
 * encoded identifier stored exactly as generated (for example
 * OUT%2F26-27%2F001).
 */
@Service
public class OutwardNumberService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final OutwardSequenceRepository sequenceRepository;
    private final OutwardRequestRepository outwardRequestRepository;
    private final Clock clock;

    @Autowired
    public OutwardNumberService(OutwardSequenceRepository sequenceRepository,
                                OutwardRequestRepository outwardRequestRepository) {
        this(sequenceRepository, outwardRequestRepository, Clock.system(BUSINESS_ZONE));
    }

    OutwardNumberService(OutwardSequenceRepository sequenceRepository,
                         OutwardRequestRepository outwardRequestRepository,
                         Clock clock) {
        this.sequenceRepository = sequenceRepository;
        this.outwardRequestRepository = outwardRequestRepository;
        this.clock = clock;
    }

    public String nextNumber() {
        int fiscalYearStart = fiscalYearStart(LocalDate.now(clock));
        String fiscalYearKey = String.format("OUTWARD:%04d-%02d", fiscalYearStart,
                (fiscalYearStart + 1) % 100);

        long sequence = sequenceRepository.nextSequence(fiscalYearKey);
        while (true) {
            String candidate = OutwardNumberGenerator.generate(fiscalYearStart, sequence);

            // Protect against a sequence document being behind already persisted
            // outward requests, matching the defensive behavior of GRN numbering.
            if (!outwardRequestRepository.existsByOutwardNumber(candidate)) {
                return candidate;
            }

            sequence = sequenceRepository.nextSequence(fiscalYearKey);
        }
    }

    static int fiscalYearStart(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }
}
