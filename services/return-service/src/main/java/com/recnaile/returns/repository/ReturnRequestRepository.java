package com.recnaile.returns.repository;

import com.recnaile.returns.model.entity.ReturnRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.List;
import com.recnaile.returns.model.enums.ReturnStatus;

public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, String> {
    Optional<ReturnRequest> findByReturnId(String returnId);
    boolean existsByReturnId(String returnId);

    List<ReturnRequest> findByOutwardIdAndItemIdAndStatusIn(String outwardId, String itemId, List<ReturnStatus> statuses);
}
