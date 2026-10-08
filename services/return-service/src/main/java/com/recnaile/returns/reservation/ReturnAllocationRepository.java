package com.recnaile.returns.reservation;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ReturnAllocationRepository extends MongoRepository<ReturnAllocation, String> {
    Optional<ReturnAllocation> findByOutwardIdAndItemId(String outwardId, String itemId);
}
