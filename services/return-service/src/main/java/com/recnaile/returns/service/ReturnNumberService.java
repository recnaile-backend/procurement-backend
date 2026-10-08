package com.recnaile.returns.service;

import com.recnaile.returns.domain.number.ReturnNumberGenerator;
import com.recnaile.returns.repository.ReturnRequestRepository;
import com.recnaile.returns.repository.ReturnSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class ReturnNumberService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final ReturnSequenceRepository sequenceRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final Clock clock;

    @Autowired
    public ReturnNumberService(
            ReturnSequenceRepository sequenceRepository,
            ReturnRequestRepository returnRequestRepository) {
        this(sequenceRepository, returnRequestRepository, Clock.system(BUSINESS_ZONE));
    }

    ReturnNumberService(
            ReturnSequenceRepository sequenceRepository,
            ReturnRequestRepository returnRequestRepository,
            Clock clock) {
        this.sequenceRepository = sequenceRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.clock = clock;
    }

    public String nextNumber() {
        int fiscalYearStart = fiscalYearStart(LocalDate.now(clock));
        String key = String.format("RETURN:%02d-%02d", fiscalYearStart % 100, (fiscalYearStart + 1) % 100);
        long sequence = sequenceRepository.nextSequence(key);

        while (true) {
            String candidate = ReturnNumberGenerator.generate(fiscalYearStart, sequence);
            if (!returnRequestRepository.existsByReturnId(candidate)) {
                return candidate;
            }
            sequence = sequenceRepository.nextSequence(key);
        }
    }

    static int fiscalYearStart(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }
}
