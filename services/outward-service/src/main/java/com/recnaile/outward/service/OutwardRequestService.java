package com.recnaile.outward.service;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardItem;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.domain.model.RecipientDetails;
import com.recnaile.outward.dto.CreateOutwardRequest;
import com.recnaile.outward.dto.OutwardItemResponse;
import com.recnaile.outward.dto.OutwardResponse;
import com.recnaile.outward.dto.RecipientResponse;
import com.recnaile.outward.repository.OutwardRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OutwardRequestService {

    private final OutwardRequestRepository repository;
    private final OutwardNumberService numberService;
    private final MasterDataValidationService masterDataValidationService;
    private final Clock clock;

    @Autowired
    public OutwardRequestService(OutwardRequestRepository repository,
                                  OutwardNumberService numberService,
                                  MasterDataValidationService masterDataValidationService) {
        this(repository, numberService, masterDataValidationService, Clock.systemUTC());
    }

    OutwardRequestService(OutwardRequestRepository repository,
                          OutwardNumberService numberService,
                          MasterDataValidationService masterDataValidationService,
                          Clock clock) {
        this.repository = repository;
        this.numberService = numberService;
        this.masterDataValidationService = masterDataValidationService;
        this.clock = clock;
    }

    public OutwardResponse create(CreateOutwardRequest request, String requestedBy) {
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        MasterDataValidationService.ValidationResult validation =
                masterDataValidationService.validate(request);

        Instant now = Instant.now(clock);
        OutwardRequest entity = new OutwardRequest();
        entity.setOutwardNumber(numberService.nextNumber());
        entity.setRequestedAt(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setItems(request.getItems().stream()
                .map(item -> new OutwardItem(item.getItemId().trim(), item.getQuantity()))
                .collect(Collectors.toList()));
        entity.setProjectId(trimToNull(request.getProjectId()));
        entity.setRequestedBy(requestedBy);
        entity.setDepartmentName(request.getDepartmentName().trim());
        entity.setPurpose(request.getPurpose().trim());
        entity.setRecipient(new RecipientDetails(
                request.getRecipient().getEmployeeId().trim(),
                request.getRecipient().getEmployeeName().trim()));
        entity.setStatus(OutwardStatus.DRAFT);
        entity.setAssetTracked(validation.assetTracked());

        return toResponse(repository.save(entity));
    }

    private OutwardResponse toResponse(OutwardRequest entity) {
        List<OutwardItemResponse> items = entity.getItems().stream()
                .map(item -> new OutwardItemResponse(item.getItemId(), item.getQuantity()))
                .toList();
        RecipientDetails recipient = entity.getRecipient();
        return new OutwardResponse(
                entity.getId(), entity.getOutwardNumber(), entity.getRequestedAt(), items,
                entity.getProjectId(), entity.getRequestedBy(), entity.getDepartmentName(),
                entity.getPurpose(), new RecipientResponse(recipient.getEmployeeId(), recipient.getEmployeeName()),
                entity.getStatus(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
