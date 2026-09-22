package com.recnaile.purchaseorder.service;

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
        String id = normalize(poId);
        return repository.findCurrentByPoId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PO_NOT_FOUND", "Purchase Order not found: " + id));
    }

    /**
     * GET-all supports only vendorName, projectId and departmentName.
     * Filters are combined with AND semantics.
     *
     * Legacy CLOSED records are excluded at repository level because CLOSED is
     * no longer a valid PurchaseOrderStatus in the current lifecycle.
     */
    public List<PurchaseOrder> find(String vendorName, String projectId, String departmentName) {
        boolean hasVendor = hasText(vendorName);
        boolean hasProject = hasText(projectId);
        boolean hasDepartment = hasText(departmentName);

        if (hasVendor && hasProject && hasDepartment) {
            return repository.findCurrentByVendorNameAndProjectIdAndDepartmentName(
                    vendorName.trim(), projectId.trim(), departmentName.trim());
        }
        if (hasVendor && hasProject) {
            return repository.findCurrentByVendorNameAndProjectId(
                    vendorName.trim(), projectId.trim());
        }
        if (hasVendor && hasDepartment) {
            return repository.findCurrentByVendorNameAndDepartmentName(
                    vendorName.trim(), departmentName.trim());
        }
        if (hasProject && hasDepartment) {
            return repository.findCurrentByProjectIdAndDepartmentName(
                    projectId.trim(), departmentName.trim());
        }
        if (hasVendor) {
            return repository.findCurrentByVendorName(vendorName.trim());
        }
        if (hasProject) {
            return repository.findCurrentByProjectId(projectId.trim());
        }
        if (hasDepartment) {
            return repository.findCurrentByDepartmentName(departmentName.trim());
        }
        return repository.findAllCurrent();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (!hasText(value)) {
            throw new ResourceNotFoundException("PO_ID_REQUIRED", "Purchase Order ID is required");
        }
        return value.trim();
    }
}
