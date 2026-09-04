package com.procurement.platform.returns;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<RecordDocument, String> {}
