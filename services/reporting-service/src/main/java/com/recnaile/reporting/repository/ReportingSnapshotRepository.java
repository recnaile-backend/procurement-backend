package com.recnaile.reporting.repository;

import com.recnaile.reporting.model.ReportingSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportingSnapshotRepository extends MongoRepository<ReportingSnapshot,String> {}
