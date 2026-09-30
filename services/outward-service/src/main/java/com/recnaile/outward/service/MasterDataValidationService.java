package com.recnaile.outward.service;

import io.micrometer.observation.annotation.Observed;
import com.recnaile.outward.client.masterdata.DepartmentResponse;
import com.recnaile.outward.client.masterdata.ItemResponse;
import com.recnaile.outward.client.masterdata.MasterDataClient;
import com.recnaile.outward.client.masterdata.ProjectResponse;
import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.exception.MasterDataValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class MasterDataValidationService {

    private final MasterDataClient masterDataClient;

    public MasterDataValidationService(MasterDataClient masterDataClient) {
        this.masterDataClient = masterDataClient;
    }

    @Observed(name = "outward.master-data.validate", contextualName = "validate-master-data")
    public ValidationResult validate(CreateOutwardRequest request) {
        validateDepartment(request.getDepartmentName());
        validateProject(request.getProjectId());

        boolean assetTracked = false;
        Set<String> itemIds = new HashSet<>();

        for (var requestedItem : request.getItems()) {
            String itemId = requestedItem.getItemId().trim();
            if (!itemIds.add(itemId)) {
                throw new MasterDataValidationException(
                        "OUTWARD_DUPLICATE_ITEM",
                        "Duplicate item in outward request: " + itemId);
            }

            ItemResponse item = masterDataClient.getItem(itemId);
            if (item == null) {
                throw new MasterDataValidationException(
                        "OUTWARD_ITEM_NOT_FOUND",
                        "Item not found in master data: " + itemId);
            }
            if (!isActive(item.status())) {
                throw new MasterDataValidationException(
                        "OUTWARD_ITEM_INACTIVE",
                        "Item is inactive in master data: " + itemId);
            }
            if (item.availableStock() == null) {
                throw new MasterDataValidationException(
                        "OUTWARD_ITEM_STOCK_UNAVAILABLE",
                        "Available stock is not configured for item: " + itemId);
            }
            if (requestedItem.getQuantity().compareTo(item.availableStock()) > 0) {
                throw new MasterDataValidationException(
                        "OUTWARD_INSUFFICIENT_STOCK",
                        "Requested quantity exceeds available stock for item: " + itemId);
            }

            assetTracked = assetTracked || item.assetTracked();
        }

        return new ValidationResult(assetTracked);
    }

    private void validateDepartment(String departmentName) {
        String requested = departmentName.trim();
        boolean found = masterDataClient.getDepartments().stream()
                .anyMatch(department -> isActive(department.status())
                        && department.departmentName() != null
                        && department.departmentName().trim().equalsIgnoreCase(requested));
        if (!found) {
            throw new MasterDataValidationException(
                    "OUTWARD_DEPARTMENT_NOT_FOUND",
                    "Active department not found in master data: " + requested);
        }
    }

    private void validateProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return;
        }
        ProjectResponse project = masterDataClient.getProject(projectId.trim());
        if (project == null) {
            throw new MasterDataValidationException(
                    "OUTWARD_PROJECT_NOT_FOUND",
                    "Project not found in master data: " + projectId.trim());
        }
        if (!isActive(project.status())) {
            throw new MasterDataValidationException(
                    "OUTWARD_PROJECT_INACTIVE",
                    "Project is inactive in master data: " + projectId.trim());
        }
    }

    private boolean isActive(String status) {
        return status != null && "ACTIVE".equalsIgnoreCase(status.trim());
    }

    public record ValidationResult(boolean assetTracked) {
    }
}
