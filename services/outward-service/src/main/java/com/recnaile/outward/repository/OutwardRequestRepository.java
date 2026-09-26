package com.recnaile.outward.repository;

import com.recnaile.outward.domain.model.OutwardRequest;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutwardRequestRepository extends MongoRepository<OutwardRequest, String> {

    Optional<OutwardRequest> findByOutwardNumber(String outwardNumber);
}
