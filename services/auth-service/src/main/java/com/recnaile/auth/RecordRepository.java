package com.procurement.platform.auth;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<RecordDocument, String> {}
