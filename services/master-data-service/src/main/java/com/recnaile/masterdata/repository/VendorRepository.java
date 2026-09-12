package com.recnaile.masterdata.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.recnaile.masterdata.model.MasterDataStatus;
import com.recnaile.masterdata.model.Vendor;

public interface VendorRepository extends MongoRepository<Vendor, String> {

    Optional<Vendor> findByName(String name);

    boolean existsByName(String name);

    boolean existsByIdAndStatus(String id, MasterDataStatus status);
}
