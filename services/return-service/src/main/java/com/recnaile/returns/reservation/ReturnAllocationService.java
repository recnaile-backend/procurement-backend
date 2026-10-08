package com.recnaile.returns.reservation;

import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reserves accepted return quantity and prevents concurrent over-return. */
@Service
public class ReturnAllocationService {
    private final ReturnAllocationRepository repository;
    public ReturnAllocationService(ReturnAllocationRepository repository) { this.repository = repository; }

    @Transactional
    public void reserve(ReturnRequest request, int issuedQuantity) {
        if (request == null) throw new IllegalArgumentException("returnRequest must not be null");
        if (issuedQuantity < 1) throw new ReturnRequestException("Issued quantity must be greater than zero");
        String outwardId = request.getOutwardId().trim();
        String itemId = request.getItemId().trim();
        int quantity = request.getQuantity();
        ReturnAllocation allocation = repository.findByOutwardIdAndItemId(outwardId, itemId).orElse(null);
        if (allocation == null) {
            repository.save(new ReturnAllocation(outwardId, itemId, issuedQuantity, quantity));
            return;
        }
        if (!Integer.valueOf(issuedQuantity).equals(allocation.getIssuedQuantity())) {
            throw new ReturnRequestException("Issued quantity changed for the outward item");
        }
        try {
            allocation.reserve(quantity);
            repository.save(allocation);
        } catch (IllegalStateException ex) {
            throw new ReturnRequestException(ex.getMessage());
        }
    }
}
