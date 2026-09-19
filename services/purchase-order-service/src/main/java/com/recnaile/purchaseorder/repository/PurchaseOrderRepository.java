package com.recnaile.purchaseorder.repository;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends MongoRepository<PurchaseOrder, String> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    List<PurchaseOrder> findByVendorIdAndStatus(String vendorId, PurchaseOrderStatus status);

    List<PurchaseOrder> findByVendorId(String vendorId);

    List<PurchaseOrder> findByProjectIdAndStatus(String projectId, PurchaseOrderStatus status);

    List<PurchaseOrder> findByProjectId(String projectId);

    List<PurchaseOrder> findBySourceIndentIdsIn(Collection<String> sourceIndentIds);
}
