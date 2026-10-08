package com.recnaile.asset.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.asset.model.Asset;

public interface AssetRepository extends MongoRepository<Asset, String> {

    List<Asset> findByOutwardNumberOrderByAssetIdAsc(String outwardNumber);

    List<Asset> findByOutwardNumberAndItemIdOrderByAssetIdAsc(String outwardNumber, String itemId);
}
