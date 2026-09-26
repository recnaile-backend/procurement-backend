package com.recnaile.outward.repository;

import com.recnaile.outward.domain.model.OutwardIssueTransaction;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OutwardIssueTransactionRepository extends MongoRepository<OutwardIssueTransaction, String> {

    Optional<OutwardIssueTransaction> findByOutwardNumber(String outwardNumber);
}
