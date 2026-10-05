package com.recnaile.returns.integration.outward;

import com.recnaile.returns.dto.IssuedReturnValidation;

public interface IssuedReturnValidationClient {
    IssuedReturnValidation validateIssuedLine(String outwardId, String outwardLineId);
}
