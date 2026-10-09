package com.recnaile.inventory.masterdata;

import java.util.List;
import java.util.Optional;

public interface MasterDataItemClient {
    Optional<MasterDataItemResponse> findById(String itemId);
    List<MasterDataItemResponse> findAll();
    Optional<MasterDataItemResponse> findUniqueByName(String name);
}
