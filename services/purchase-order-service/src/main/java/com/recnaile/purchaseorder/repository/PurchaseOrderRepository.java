package com.recnaile.purchaseorder.repository;

import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends MongoRepository<PurchaseOrder, String> {

    /*
     * CLOSED was removed from the current PO lifecycle. These queries deliberately
     * exclude legacy CLOSED documents that may still exist in MongoDB from an older
     * deployment. Without this guard, Spring Data tries to deserialize the legacy
     * "CLOSED" enum value into the current PurchaseOrderStatus enum and the entire
     * GET-all request can fail with HTTP 500.
     */
    @Query("{ '_id': ?0, 'status': { '$ne': 'CLOSED' } }")
    Optional<PurchaseOrder> findCurrentByPoId(String poId);

    @Query("{ 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findAllCurrent();

    @Query("{ 'vendorName': ?0, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByVendorName(String vendorName);

    @Query("{ 'projectId': ?0, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByProjectId(String projectId);

    @Query("{ 'departmentName': ?0, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByDepartmentName(String departmentName);

    @Query("{ 'vendorName': ?0, 'projectId': ?1, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByVendorNameAndProjectId(String vendorName, String projectId);

    @Query("{ 'vendorName': ?0, 'departmentName': ?1, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByVendorNameAndDepartmentName(String vendorName, String departmentName);

    @Query("{ 'projectId': ?0, 'departmentName': ?1, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByProjectIdAndDepartmentName(String projectId, String departmentName);

    @Query("{ 'vendorName': ?0, 'projectId': ?1, 'departmentName': ?2, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentByVendorNameAndProjectIdAndDepartmentName(
            String vendorName, String projectId, String departmentName);

    @Query("{ 'sourceIndentIds': { '$in': ?0 }, 'status': { '$ne': 'CLOSED' } }")
    List<PurchaseOrder> findCurrentBySourceIndentIdsIn(Collection<String> sourceIndentIds);
}
