package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.repository.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReturnRequestService {

    private final ReturnRequestRepository repository;
    private final Clock clock;

    @Autowired
    public ReturnRequestService(ReturnRequestRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ReturnRequestService(ReturnRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public ReturnRequest create(CreateReturnRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        ReturnRequest returnRequest = new ReturnRequest(
                request.outwardId().trim(),
                request.outwardLineId().trim(),
                request.itemId().trim(),
                request.quantity(),
                request.returnedBy().trim(),
                request.condition(),
                normalizeRemarks(request.remarks()),
                normalizeOptional(request.assetId()),
                Instant.now(clock));

        return repository.save(returnRequest);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRemarks(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
