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
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutwardRequestService {

    private final OutwardRequestRepository repository;
    private final OutwardNumberService numberService;
    private final MasterDataValidationService masterDataValidationService;
    private final com.recnaile.outward.outbox.AuditEventService auditEventService;
    private final Clock clock;

    @Autowired
    public OutwardRequestService(OutwardRequestRepository repository,
                                  OutwardNumberService numberService,
                                  MasterDataValidationService masterDataValidationService,
                                  com.recnaile.outward.outbox.AuditEventService auditEventService) {
        this(repository, numberService, masterDataValidationService, auditEventService, Clock.systemUTC());
    }

    OutwardRequestService(OutwardRequestRepository repository,
                          OutwardNumberService numberService,
                          MasterDataValidationService masterDataValidationService,
                          com.recnaile.outward.outbox.AuditEventService auditEventService,
                          Clock clock) {
        this.repository = repository;
        this.numberService = numberService;
        this.masterDataValidationService = masterDataValidationService;
        this.auditEventService = auditEventService;
        this.clock = clock;
    }

    public OutwardResponse create(CreateOutwardRequest request, String requestedBy) {
        return create(request, requestedBy, null);
    }

    @Observed(name = "outward.request.create", contextualName = "create-outward-request")
    @Transactional
    public OutwardResponse create(CreateOutwardRequest request, String requestedBy, Authentication authentication) {
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
                .map(item -> new OutwardItem(item.getItemId().trim(), item.getQuantity(),
                        validation.assetTrackedByItem().getOrDefault(item.getItemId().trim(), false)))
                .collect(Collectors.toList()));
        entity.setProjectId(trimToNull(request.getProjectId()));
        entity.setRequestedBy(requestedBy);
        entity.setDepartmentName(request.getDepartmentName().trim());
        entity.setPurpose(request.getPurpose().trim());
        entity.setRecipient(new RecipientDetails(
                request.getRecipient().getEmployeeId().trim(),
                request.getRecipient().getEmployeeName().trim()));
        entity.setStatus(OutwardStatus.PENDING_APPROVAL);
        entity.setAssetTracked(validation.assetTracked());

        OutwardRequest saved = repository.save(entity);
        auditEventService.record("OUTWARD_REQUEST", saved.getOutwardNumber(), "CREATE",
                authentication, null, saved.getStatus().name(), null);
        return toResponse(saved);
    }

    @Observed(name = "outward.request.get.all", contextualName = "get-all-outward-requests")
    @Transactional(readOnly = true)
    public List<OutwardResponse> getAll() {
        return repository.findAllByOrderByRequestedAtDesc().stream()
                .map(this::toResponse)
                .toList();
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
                entity.getStatus(), entity.getApprovedBy(), entity.getApprovedAt(), entity.getRejectedBy(),
                entity.getRejectedAt(), entity.getRejectionReason(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
