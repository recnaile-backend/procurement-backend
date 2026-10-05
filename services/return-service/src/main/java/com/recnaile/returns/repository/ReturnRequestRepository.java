package com.recnaile.returns.repository;

import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, String> {
    Optional<ReturnRequest> findByReturnId(String returnId);
    boolean existsByReturnId(String returnId);
}
