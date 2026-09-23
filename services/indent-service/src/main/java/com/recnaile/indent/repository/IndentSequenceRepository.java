package com.recnaile.indent.repository;

import com.recnaile.indent.domain.model.IndentSequence;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IndentSequenceRepository extends MongoRepository<IndentSequence, String> {
}
