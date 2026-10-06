package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.integration.outward.IssuedReturnValidationClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IssuedReturnValidationService {

    private final IssuedReturnValidationClient client;

    public IssuedReturnValidationService(IssuedReturnValidationClient client) {
        this.client = client;
    }

    public void validate(CreateReturnRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        IssuedReturnValidation issued = client.validateIssuedItem(
                request.outwardId().trim(), request.itemId().trim());

        if (issued == null) {
            throw new ReturnRequestException("Issued outward item was not found");
        }
        if (!Objects.equals(request.outwardId().trim(), issued.outwardId())) {
            throw new ReturnRequestException("Outward ID does not match the issued item");
        }
        if (!Objects.equals(request.itemId().trim(), issued.itemId())) {
            throw new ReturnRequestException("Item ID does not match the issued item");
        }
        if (issued.issuedQuantity() == null || issued.issuedQuantity() <= 0) {
            throw new ReturnRequestException("Issued quantity must be greater than zero");
        }

        int remaining = issued.remainingQuantity();
        if (remaining <= 0) {
            throw new ReturnRequestException("No quantity remains available for return");
        }
        if (request.quantity() > remaining) {
            throw new ReturnRequestException("Return quantity exceeds the remaining issued quantity");
        }
    }
}
