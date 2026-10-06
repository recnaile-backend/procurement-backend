package com.recnaile.asset.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.asset.model.Asset;

public interface AssetRepository extends MongoRepository<Asset, String> {

    Optional<Asset> findByIssueReference(String issueReference);
}
