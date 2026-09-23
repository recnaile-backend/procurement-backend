package com.recnaile.inventory.repository;

import com.recnaile.inventory.domain.InventoryAuditDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryAuditRepository extends MongoRepository<InventoryAuditDocument,String> {}
