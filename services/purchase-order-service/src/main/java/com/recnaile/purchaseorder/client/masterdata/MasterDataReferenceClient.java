package com.recnaile.purchaseorder.client.masterdata;

public interface MasterDataReferenceClient {

    DepartmentResponse requireActiveDepartmentByName(String departmentName);
}
