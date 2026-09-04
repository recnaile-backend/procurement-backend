package com.procurement.platform.reporting;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<RecordDocument, String> {}
