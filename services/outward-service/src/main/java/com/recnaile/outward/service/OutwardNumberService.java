package com.recnaile.outward.service;

import com.recnaile.outward.domain.number.OutwardNumberGenerator;
import com.recnaile.outward.repository.OutwardSequenceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OutwardNumberService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final OutwardSequenceRepository sequenceRepository;
    private final Clock clock;

    @Autowired
    public OutwardNumberService(OutwardSequenceRepository sequenceRepository) {
        this(sequenceRepository, Clock.system(BUSINESS_ZONE));
    }

    OutwardNumberService(OutwardSequenceRepository sequenceRepository, Clock clock) {
        this.sequenceRepository = sequenceRepository;
        this.clock = clock;
    }

    public String nextNumber() {
        int fiscalYearStart = fiscalYearStart(LocalDate.now(clock));
        String fiscalYearKey = String.format("OUTWARD:%04d-%02d", fiscalYearStart,
                (fiscalYearStart + 1) % 100);
        long sequence = sequenceRepository.nextSequence(fiscalYearKey);
        return OutwardNumberGenerator.generate(fiscalYearStart, sequence);
    }

    static int fiscalYearStart(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }
}
