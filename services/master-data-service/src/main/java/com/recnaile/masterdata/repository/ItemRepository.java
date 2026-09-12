package com.recnaile.masterdata.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.masterdata.model.Item;
import com.recnaile.masterdata.model.MasterDataStatus;

public interface ItemRepository extends MongoRepository<Item, String> {

    boolean existsByItemIdAndStatus(String itemId, MasterDataStatus status);
}
