package com.recnaile.returns.integration.outward;

import com.recnaile.returns.dto.IssuedReturnValidation;

public interface IssuedReturnValidationClient {
    IssuedReturnValidation validateIssuedItem(String outwardId, String itemId);
}
