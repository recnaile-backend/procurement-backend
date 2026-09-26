package com.recnaile.inward.po;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PurchaseOrderApiResponse<T>(
        Boolean success,
        T data,
        Object error
) {
}
