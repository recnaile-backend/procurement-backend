package com.recnaile.audit.api;

import com.recnaile.audit.application.AuditEventQueryService;
import com.recnaile.audit.persistence.AuditEventDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit Events", description = "Read-only access to immutable audit history")
public class AuditEventController {
    private static final int MAX_PAGE_SIZE = 100;
    private final AuditEventQueryService queryService;

    public AuditEventController(AuditEventQueryService queryService) { this.queryService = queryService; }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get an audit event by its UUID")
    public ResponseEntity<ApiResponse<AuditEventDocument>> getById(@PathVariable UUID eventId) {
        return queryService.findByEventId(eventId)
                .map(event -> ResponseEntity.ok(ApiResponse.success(event)))
                .orElseGet(() -> ResponseEntity.status(404).body(
                        ApiResponse.<AuditEventDocument>failure("NOT_FOUND", "Audit event was not found", null)));
    }

    @GetMapping("/entities/{entityType}/{entityId}")
    @Operation(summary = "Get paginated audit history for an entity")
    public ApiResponse<PaginatedData<AuditEventDocument>> entityHistory(
            @PathVariable String entityType, @PathVariable String entityId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100") @RequestParam(defaultValue = "20") int size) {
        return page(queryService.findEntityHistory(entityType, entityId, pageable(page, size)));
    }

    @GetMapping("/actors/{actorId}")
    @Operation(summary = "Get paginated audit history for an actor")
    public ApiResponse<PaginatedData<AuditEventDocument>> actorHistory(
            @PathVariable String actorId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(queryService.findActorHistory(actorId, pageable(page, size)));
    }

    @GetMapping("/correlations/{correlationId}")
    @Operation(summary = "Find audit events by correlation ID")
    public ApiResponse<PaginatedData<AuditEventDocument>> correlationHistory(
            @PathVariable String correlationId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(queryService.findCorrelationHistory(correlationId, pageable(page, size)));
    }

    @GetMapping("/traces/{traceId}")
    @Operation(summary = "Find audit events by trace ID")
    public ApiResponse<PaginatedData<AuditEventDocument>> traceHistory(
            @PathVariable String traceId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(queryService.findTraceHistory(traceId, pageable(page, size)));
    }

    @GetMapping("/services/{service}/actions/{action}")
    @Operation(summary = "Find audit events by producer service and action")
    public ApiResponse<PaginatedData<AuditEventDocument>> serviceActionHistory(
            @PathVariable String service, @PathVariable String action,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
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
