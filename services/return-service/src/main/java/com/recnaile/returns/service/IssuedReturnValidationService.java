package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.integration.outward.IssuedReturnValidationClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        IssuedReturnValidation issued = client.validateIssuedLine(
                request.outwardId().trim(), request.outwardLineId().trim());

        if (issued == null) {
            throw new ReturnRequestException("Issued outward line was not found");
        }
        if (!Objects.equals(request.outwardId().trim(), issued.outwardId())) {
            throw new ReturnRequestException("Outward ID does not match the issued line");
        }
        if (!Objects.equals(request.outwardLineId().trim(), issued.outwardLineId())) {
            throw new ReturnRequestException("Outward line ID does not match the issued line");
        }
        if (!Objects.equals(request.itemId().trim(), issued.itemId())) {
            throw new ReturnRequestException("Item ID does not match the issued line");
        }
        if (issued.issuedQuantity() == null || issued.issuedQuantity().signum() <= 0) {
            throw new ReturnRequestException("Issued quantity must be greater than zero");
        }

        BigDecimal remaining = issued.remainingQuantity();
        if (remaining.signum() <= 0) {
            throw new ReturnRequestException("No quantity remains available for return");
        }
        if (request.quantity().compareTo(remaining) > 0) {
            throw new ReturnRequestException("Return quantity exceeds the remaining issued quantity");
        }

        String issuedAssetId = normalize(issued.assetId());
        String requestedAssetId = normalize(request.assetId());

        if (issuedAssetId != null && !Objects.equals(issuedAssetId, requestedAssetId)) {
            throw new ReturnRequestException("Asset ID does not match the issued asset");
        }
        if (issuedAssetId == null && requestedAssetId != null) {
            throw new ReturnRequestException("Return asset is not linked to the issued line");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
