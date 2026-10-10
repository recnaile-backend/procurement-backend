package com.recnaile.audit.api;

import com.recnaile.audit.application.AuditEventQueryService;
import com.recnaile.audit.persistence.AuditEventDocument;
import com.recnaile.audit.security.AuditAccessPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit Events", description = "Read-only access to immutable audit history")
public class AuditEventController {
    private static final int MAX_PAGE_SIZE = 100;
    private final AuditEventQueryService queryService;
    private final AuditAccessPolicy accessPolicy;

    public AuditEventController(AuditEventQueryService queryService, AuditAccessPolicy accessPolicy) {
        this.queryService = queryService;
        this.accessPolicy = accessPolicy;
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get an audit event by its UUID")
    public ResponseEntity<ApiResponse<AuditEventDocument>> getById(@PathVariable UUID eventId, @AuthenticationPrincipal Jwt jwt) {
        var event = queryService.findByEventId(eventId);
        if (event.isEmpty()) return ResponseEntity.status(404).body(
                ApiResponse.failure("NOT_FOUND", "Audit event was not found", null));
        accessPolicy.requireEventAccess(event.get(), jwt);
        return ResponseEntity.ok(ApiResponse.success(event.get()));
    }

    @GetMapping("/entities/{entityType}/{entityId}")
    @Operation(summary = "Get paginated audit history for an entity; PROCUREMENT and MD only")
    public ApiResponse<PaginatedData<AuditEventDocument>> entityHistory(
            @PathVariable String entityType, @PathVariable String entityId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        accessPolicy.requireGlobalAuditRead(jwt);
        return page(queryService.findEntityHistory(entityType, entityId, pageable(page, size)));
    }

    @GetMapping("/actors/{actorId}")
    @Operation(summary = "Get paginated audit history for an actor; EMPLOYEE/TL limited to own history")
    public ApiResponse<PaginatedData<AuditEventDocument>> actorHistory(
            @PathVariable String actorId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal Jwt jwt) {
        accessPolicy.requireActorHistoryAccess(actorId, jwt);
        return page(queryService.findActorHistory(actorId, pageable(page, size)));
    }

    @GetMapping("/correlations/{correlationId}")
    @Operation(summary = "Find audit events by correlation ID; PROCUREMENT and MD only")
    public ApiResponse<PaginatedData<AuditEventDocument>> correlationHistory(
            @PathVariable String correlationId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal Jwt jwt) {
        accessPolicy.requireGlobalAuditRead(jwt);
        return page(queryService.findCorrelationHistory(correlationId, pageable(page, size)));
    }

    @GetMapping("/traces/{traceId}")
    @Operation(summary = "Find audit events by trace ID; PROCUREMENT and MD only")
    public ApiResponse<PaginatedData<AuditEventDocument>> traceHistory(
            @PathVariable String traceId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal Jwt jwt) {
        accessPolicy.requireGlobalAuditRead(jwt);
        return page(queryService.findTraceHistory(traceId, pageable(page, size)));
    }

    @GetMapping("/services/{service}/actions/{action}")
    @Operation(summary = "Find audit events by producer service and action; PROCUREMENT and MD only")
    public ApiResponse<PaginatedData<AuditEventDocument>> serviceActionHistory(
            @PathVariable String service, @PathVariable String action,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        accessPolicy.requireGlobalAuditRead(jwt);
        return page(queryService.findServiceActionHistory(service, action, pageable(page, size)));
    }

    private static Pageable pageable(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (size < 1 || size > MAX_PAGE_SIZE) throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
    }
    private static ApiResponse<PaginatedData<AuditEventDocument>> page(Page<AuditEventDocument> result) {
        return ApiResponse.success(PaginatedData.from(result));
    }
}
