package com.recnaile.outward.service;

import java.util.List;

import com.recnaile.outward.domain.enums.OutwardStatus;
import com.recnaile.outward.domain.model.OutwardRequest;
import com.recnaile.outward.dto.response.OutwardResponse;
import com.recnaile.outward.exception.OutwardNotFoundException;
import com.recnaile.outward.repository.OutwardRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class OutwardQueryService {

    private final OutwardRepository repository;

    public OutwardQueryService(OutwardRepository repository) {
        this.repository = repository;
    }

    public OutwardResponse get(String outwardId, Authentication authentication) {
        OutwardRequest outward = repository.findByOutwardId(outwardId)
                .orElseThrow(() -> new OutwardNotFoundException(outwardId));
        if (isEmployee(authentication)
                && !authentication.getName().equals(outward.getRequesterId())
                && !authentication.getName().equals(outward.getRecipientUserId())) {
            throw new com.recnaile.outward.exception.OutwardException(
                    "ACCESS_DENIED", "You are not authorized to view this outward request");
        }
        return toResponse(outward);
    }

    public List<OutwardResponse> all(int limit) {
        return repository.findAll(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)))
                .stream().map(this::toResponse).toList();
    }

    private boolean isEmployee(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EMPLOYEE".equals(a.getAuthority()));
    }

    public List<OutwardResponse> byStatus(OutwardStatus status, int limit) {
        return repository.findByStatusOrderByCreatedAtDesc(status,
                        PageRequest.of(0, Math.min(Math.max(limit, 1), 200)))
                .stream().map(this::toResponse).toList();
    }

    public List<OutwardResponse> mine(String requesterId, int limit) {
        return repository.findByRequesterIdOrderByCreatedAtDesc(requesterId,
                        PageRequest.of(0, Math.min(Math.max(limit, 1), 200)))
                .stream().map(this::toResponse).toList();
    }

    public OutwardResponse toResponse(OutwardRequest x) {
        return new OutwardResponse(
                x.getOutwardId(), x.getItemId(), x.getQuantity(),
                x.getRecipientUserId(), x.getRecipientName(),
                x.getDepartmentId(), x.getProjectId(), x.getPurpose(), x.getLocation(),
                x.getRequesterId(), x.getStatus(), x.getApproverId(), x.getApproverRole(),
                x.getApprovedAt(), x.getRejectionReason(), x.getIssuerId(), x.getIssuedAt(),
                x.getRequestedAt(), x.isAssetTracked(), x.getVersion(),
                x.getCreatedAt(), x.getUpdatedAt());
    }
}
