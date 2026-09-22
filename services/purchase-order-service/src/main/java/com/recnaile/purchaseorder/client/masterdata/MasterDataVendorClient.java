package com.recnaile.purchaseorder.client.masterdata;

import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.exception.MasterDataIntegrationException;

import java.util.List;

public interface MasterDataVendorClient {

    VendorResponse getVendor(String vendorId);

    List<VendorResponse> getAllVendors();

    default VendorResponse requireActiveVendor(String vendorId) {
        VendorResponse vendor = getVendor(vendorId);
        return requireActive(vendor, vendorId);
    }

    default VendorResponse requireActiveVendorByName(String vendorName) {
        if (vendorName == null || vendorName.isBlank()) {
            throw new BusinessRuleException("VENDOR_NAME_REQUIRED", "Vendor name is required");
        }

        List<VendorResponse> vendors = getAllVendors();
        if (vendors == null) {
            throw new MasterDataIntegrationException(
                    "MASTER_DATA_INVALID_RESPONSE",
                    "Master-data-service returned an empty vendor list");
        }

        String normalizedName = vendorName.trim();
        List<VendorResponse> matches = vendors.stream()
                .filter(v -> v != null && v.name() != null
                        && normalizedName.equalsIgnoreCase(v.name().trim()))
                .filter(v -> "ACTIVE".equalsIgnoreCase(v.status()))
                .toList();

        if (matches.isEmpty()) {
            throw new com.recnaile.purchaseorder.exception.ResourceNotFoundException(
                    "VENDOR_NOT_FOUND",
                    "Active vendor not found in master-data-service: " + normalizedName);
        }
        if (matches.size() > 1) {
            throw new BusinessRuleException(
                    "VENDOR_NAME_AMBIGUOUS",
                    "Multiple active vendors found with the name: " + normalizedName);
        }
        return matches.getFirst();
    }

    private static VendorResponse requireActive(VendorResponse vendor, String vendorId) {
        if (vendor == null) {
            throw new MasterDataIntegrationException(
                    "MASTER_DATA_INVALID_RESPONSE",
                    "Master-data-service returned an empty vendor response for: " + vendorId);
        }
        if (!"ACTIVE".equalsIgnoreCase(vendor.status())) {
            throw new BusinessRuleException(
                    "VENDOR_INACTIVE",
                    "Vendor is inactive and cannot be used for a purchase order: " + vendorId);
        }
        return vendor;
    }
}
