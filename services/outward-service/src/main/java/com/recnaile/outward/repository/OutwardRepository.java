package com.recnaile.outward.repository;

import java.util.List;
import java.util.Optional;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutwardRepository extends MongoRepository<OutwardRequest, String> {
    List<OutwardRequest> findByStatusOrderByCreatedAtDesc(OutwardStatus status, Pageable pageable);
    List<OutwardRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId, Pageable pageable);
    Optional<OutwardRequest> findByOutwardId(String outwardId);
}
