package com.recnaile.returns.repository;

import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, String> {
}
