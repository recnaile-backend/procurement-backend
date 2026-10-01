package com.recnaile.outward.service;

import io.micrometer.observation.annotation.Observed;
import com.recnaile.outward.client.auth.AuthEmployeeClient;
import com.recnaile.outward.client.auth.EmployeeResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Service
public class MasterDataValidationService {

    private final MasterDataClient masterDataClient;
    private final AuthEmployeeClient authEmployeeClient;

    public MasterDataValidationService(MasterDataClient masterDataClient, AuthEmployeeClient authEmployeeClient) {
        this.masterDataClient = masterDataClient;
        this.authEmployeeClient = authEmployeeClient;
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

        validateRecipient(request);
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


    private void validateRecipient(CreateOutwardRequest request) {
        if (request.getRecipient() == null) {
            throw new MasterDataValidationException(
                    "OUTWARD_RECIPIENT_NOT_FOUND",
                    "Recipient employee is required");
        }

        String employeeId = request.getRecipient().getEmployeeId() == null
                ? "" : request.getRecipient().getEmployeeId().trim();
        String requestedName = request.getRecipient().getEmployeeName() == null
                ? "" : request.getRecipient().getEmployeeName().trim();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean mdCaller = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MD".equals(authority.getAuthority()));

        if (!mdCaller) {
            String authenticatedEmployeeId = null;
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                authenticatedEmployeeId = jwtAuthentication.getToken().getClaimAsString("employeeId");
            }

            if (authenticatedEmployeeId == null || !authenticatedEmployeeId.trim().equalsIgnoreCase(employeeId)) {
                throw new MasterDataValidationException(
                        "OUTWARD_EMPLOYEE_VALIDATION_FORBIDDEN",
                        "Only an MD caller can validate an arbitrary recipient employee; non-MD callers can select only themselves");
            }

            // The current Auth JWT contains employeeId but not firstName/lastName.
            // Therefore a non-MD self-recipient can be validated by authoritative ID,
            // while the display name is retained as supplied by the caller.
            return;
        }

        EmployeeResponse employee = authEmployeeClient.getEmployees().stream()
                .filter(candidate -> candidate.getEmployeeId() != null
                        && candidate.getEmployeeId().trim().equalsIgnoreCase(employeeId))
                .findFirst()
                .orElse(null);

        if (employee == null) {
            throw new MasterDataValidationException(
                    "OUTWARD_EMPLOYEE_NOT_FOUND",
                    "Recipient employee not found: " + employeeId);
        }

        if (!employee.isActive()) {
            throw new MasterDataValidationException(
                    "OUTWARD_EMPLOYEE_INACTIVE",
                    "Recipient employee is inactive: " + employeeId);
        }

        String actualName = employee.displayName();
        if (actualName.isBlank() || !actualName.equalsIgnoreCase(requestedName)) {
            throw new MasterDataValidationException(
                    "OUTWARD_EMPLOYEE_NAME_MISMATCH",
                    "Recipient employee name does not match employee ID: " + employeeId);
        }
    }

    private boolean isActive(String status) {
        return status != null && "ACTIVE".equalsIgnoreCase(status.trim());
    }

    public record ValidationResult(boolean assetTracked) {
    }
}
