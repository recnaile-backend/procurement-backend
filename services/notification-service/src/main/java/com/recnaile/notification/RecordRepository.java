package com.recnaile.notification;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecordRepository extends MongoRepository<RecordDocument, String> {}
