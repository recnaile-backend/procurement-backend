package com.recnaile.returns.service;

import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.IssuedReturnValidation;
import com.recnaile.returns.exception.ReturnRequestException;
import com.recnaile.returns.integration.outward.IssuedReturnValidationClient;
import com.recnaile.returns.repository.ReturnRequestRepository;
import com.recnaile.returns.model.enums.ReturnStatus;
import com.recnaile.returns.model.entity.ReturnRequest;
import java.util.List;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IssuedReturnValidationService {

    private final IssuedReturnValidationClient client;
    private final ReturnRequestRepository repository;

    public IssuedReturnValidationService(IssuedReturnValidationClient client) {
        this(client, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public IssuedReturnValidationService(IssuedReturnValidationClient client, ReturnRequestRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    public IssuedReturnValidation validate(CreateReturnRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        String outwardId = request.outwardId().trim();
        String itemId = request.itemId().trim();

        IssuedReturnValidation issued = client.validateIssuedItem(outwardId, itemId);

        if (issued == null) {
            throw new ReturnRequestException("Issued outward item was not found");
        }
        if (!Objects.equals(outwardId, issued.outwardId())) {
            throw new ReturnRequestException("Outward ID does not match the issued item");
        }
        if (!Objects.equals(itemId, issued.itemId())) {
            throw new ReturnRequestException("Item ID does not match the issued item");
        }
        if (issued.issuedQuantity() == null || issued.issuedQuantity() <= 0) {
            throw new ReturnRequestException("Issued quantity must be greater than zero");
        }
        if (issued.alreadyReturnedQuantity() == null || issued.alreadyReturnedQuantity() < 0) {
            throw new ReturnRequestException("Already returned quantity must be zero or greater");
        }
        if (issued.assetTracked() == null) {
            throw new ReturnRequestException("Outward validation must provide assetTracked");
        }

        int remaining = issued.issuedQuantity() - effectiveAlreadyReturnedQuantity(outwardId, itemId, issued.alreadyReturnedQuantity());
        if (remaining <= 0) {
            throw new ReturnRequestException("No quantity remains available for return");
        }
        if (request.quantity() > remaining) {
            throw new ReturnRequestException("Return quantity exceeds the remaining issued quantity");
        }

        if (Boolean.TRUE.equals(issued.assetTracked()) && request.quantity() != 1) {
            throw new ReturnRequestException("Asset-tracked reusable returns must have quantity 1");
        }

        return issued;
    }
    public IssuedReturnValidation validate(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            throw new IllegalArgumentException("returnRequest must not be null");
        }

        IssuedReturnValidation issued = client.validateIssuedItem(
                returnRequest.getOutwardId().trim(),
                returnRequest.getItemId().trim());

        if (issued == null) {
            throw new ReturnRequestException("Issued outward item was not found");
        }
        if (!Objects.equals(returnRequest.getOutwardId().trim(), issued.outwardId())
                || !Objects.equals(returnRequest.getItemId().trim(), issued.itemId())) {
            throw new ReturnRequestException("Return no longer matches the issued outward item");
        }
        if (issued.issuedQuantity() == null || issued.issuedQuantity() <= 0) {
            throw new ReturnRequestException("Issued quantity must be greater than zero");
        }
        if (issued.alreadyReturnedQuantity() == null || issued.alreadyReturnedQuantity() < 0) {
            throw new ReturnRequestException("Already returned quantity must be zero or greater");
        }
        if (issued.assetTracked() == null) {
            throw new ReturnRequestException("Outward validation must provide assetTracked");
        }
        if (returnRequest.getAssetTracked() != null
                && !Objects.equals(Boolean.TRUE.equals(returnRequest.getAssetTracked()), issued.assetTracked())) {
            throw new ReturnRequestException("Asset tracking status changed after return creation");
        }

        int remaining = issued.issuedQuantity() - effectiveAlreadyReturnedQuantity(
                returnRequest.getOutwardId().trim(), returnRequest.getItemId().trim(), issued.alreadyReturnedQuantity());
        if (remaining <= 0) {
            throw new ReturnRequestException("No quantity remains available for return");
        }
        if (returnRequest.getQuantity() > remaining) {
            throw new ReturnRequestException("Return quantity exceeds the remaining issued quantity");
        }
        if (Boolean.TRUE.equals(issued.assetTracked()) && returnRequest.getQuantity() != 1) {
            throw new ReturnRequestException("Asset-tracked reusable returns must have quantity 1");
        }

        return issued;
    }


    private int effectiveAlreadyReturnedQuantity(String outwardId, String itemId, Integer outwardReportedQuantity) {
        if (repository == null) {
            return outwardReportedQuantity == null ? 0 : Math.max(0, outwardReportedQuantity);
        }
        List<ReturnRequest> returns = repository.findByOutwardIdAndItemIdAndStatusIn(
                outwardId, itemId, List.of(ReturnStatus.ACCEPTED_GOOD, ReturnStatus.ACCEPTED_DAMAGED));
        int total = 0;
        for (ReturnRequest request : returns) {
            if (request.getQuantity() != null) total += request.getQuantity();
        }
        return total;
    }
}