package com.recnaile.purchaseorder.service;

import com.recnaile.purchaseorder.domain.enums.PurchaseOrderStatus;
import com.recnaile.purchaseorder.domain.model.PurchaseOrder;
import com.recnaile.purchaseorder.exception.ResourceNotFoundException;
import com.recnaile.purchaseorder.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseOrderQueryService {

    private final PurchaseOrderRepository repository;

    public PurchaseOrderQueryService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    public PurchaseOrder getById(String poId) {
        return repository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PO_NOT_FOUND", "Purchase Order not found: " + poId));
    }

    public List<PurchaseOrder> find(PurchaseOrderStatus status, String vendorId, String projectId) {
        if (status != null) {
            if (vendorId != null && !vendorId.isBlank()) {
                return repository.findByVendorIdAndStatus(vendorId.trim(), status);
            }
            if (projectId != null && !projectId.isBlank()) {
                return repository.findByProjectIdAndStatus(projectId.trim(), status);
            }
            return repository.findByStatus(status);
        }

        if (vendorId != null && !vendorId.isBlank()) {
            return repository.findByVendorId(vendorId.trim());
        }
        if (projectId != null && !projectId.isBlank()) {
            return repository.findByProjectId(projectId.trim());
        }
        return repository.findAll();
    }
}
