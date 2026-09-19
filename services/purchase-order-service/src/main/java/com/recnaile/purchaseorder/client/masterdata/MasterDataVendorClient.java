package com.recnaile.purchaseorder.client.masterdata;

import com.recnaile.purchaseorder.exception.BusinessRuleException;
import com.recnaile.purchaseorder.exception.MasterDataIntegrationException;

public interface MasterDataVendorClient {

    VendorResponse getVendor(String vendorId);

    default VendorResponse requireActiveVendor(String vendorId) {
        VendorResponse vendor = getVendor(vendorId);
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
