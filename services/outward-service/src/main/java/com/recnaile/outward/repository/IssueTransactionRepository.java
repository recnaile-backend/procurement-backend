package com.recnaile.outward.repository;

import java.util.Optional;

import com.recnaile.outward.domain.model.IssueTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IssueTransactionRepository extends MongoRepository<IssueTransaction, String> {
    Optional<IssueTransaction> findByOutwardId(String outwardId);
}
