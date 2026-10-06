package com.recnaile.asset.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.asset.event.ProcessedEvent;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
}
