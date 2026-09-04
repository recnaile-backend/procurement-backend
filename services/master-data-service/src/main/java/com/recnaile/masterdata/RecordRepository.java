package com.procurement.platform.masterdata;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<RecordDocument, String> {}
