package com.recnaile.indent.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;

public interface IndentRepository
        extends MongoRepository<Indent, String>, IndentRepositoryCustom {

    Optional<Indent> findByIndentNumber(String indentNumber);

    List<Indent> findByStatusAndApprovalDueAtLessThanEqualOrderByApprovalDueAtAsc(
            IndentStatus status,
            Instant approvalDueAt,
            Pageable pageable);
}
