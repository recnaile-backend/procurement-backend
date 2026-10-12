package com.recnaile.reporting.web;

import com.recnaile.reporting.model.*;
import com.recnaile.reporting.service.ReportingQueryService;
import com.recnaile.reporting.service.ReportingRecoveryService;
import com.recnaile.reporting.service.ReportingScopePolicy;
import com.recnaile.reporting.service.ReportingSnapshotService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@RestController
@Tag(name = "Reporting", description = "Read-only dashboards and reports, plus restricted snapshot and recovery operations")
@Validated
@RequestMapping("/api/v1/reporting")
public class ReportingController {
    private final ReportingQueryService query;
    private final ReportingSnapshotService snapshots;
    private final ReportingScopePolicy scopePolicy;
    private final ReportingRecoveryService recovery;

    public ReportingController(ReportingQueryService query, ReportingSnapshotService snapshots, ReportingScopePolicy scopePolicy, ReportingRecoveryService recovery) {
        this.query = query; this.snapshots = snapshots; this.scopePolicy = scopePolicy; this.recovery = recovery;
    }

    @GetMapping("/admin/recovery-status")
    public Map<String,Object> recoveryStatus() {
        return recovery.status();
    }

    /** Destructive only to derived projections; requires explicit confirmation and preserves facts/snapshots. */
    @PostMapping("/admin/rebuild-projections")
    public Map<String,Object> rebuildProjections(@RequestParam(defaultValue="false") boolean confirm) {
        if (!confirm) throw new IllegalArgumentException("Set confirm=true to rebuild reporting projections from retained event facts");
        return recovery.rebuildProjections();
    }

    @GetMapping("/dashboard/summary")
    public Map<String,Object> dashboard(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId) {
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return query.dashboard(scope.departmentId(), scope.projectId());
    }

    @GetMapping("/dashboard/freshness")
    public ReportFreshness freshness(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId) {
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return query.freshness(scope.departmentId(), scope.projectId());
    }

    @GetMapping("/reports/snapshots")
    public Page<ReportingSnapshot> snapshots(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId,
        @RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="25") @Min(1) @Max(200) int size,
        @RequestParam(defaultValue="snapshotDate,desc") String sort) {
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return snapshots.findSnapshots(scope.departmentId(), scope.projectId(), PageRequest.of(page, size,
            parseSort(sort, Set.of("snapshotDate", "capturedAt"))));
    }

    @PostMapping("/admin/snapshots")
    public ReportingSnapshot captureSnapshot(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId) {
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return snapshots.capture(date == null ? LocalDate.now(java.time.ZoneOffset.UTC) : date,
            scope.departmentId(), scope.projectId());
    }

    @GetMapping("/reports/period-summary")
    public Map<String,Object> period(@AuthenticationPrincipal Jwt jwt,
        @RequestParam ReportPeriod period,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId) {
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return query.periodSummary(period, from, to, scope.departmentId(), scope.projectId());
    }

    @GetMapping("/reports/projections")
    public Page<ReportingProjection> projections(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) String entityType, @RequestParam(required=false) String status,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId,
        @RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="25") @Min(1) @Max(200) int size,
        @RequestParam(defaultValue="updatedAt,desc") String sort) {
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return query.projections(entityType, status, scope.departmentId(), scope.projectId(),
            PageRequest.of(page, size, parseSort(sort, Set.of("updatedAt", "createdAt", "entityType", "status"))));
    }

    @GetMapping("/reports/events")
    public Page<ReportingFact> events(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) String eventType,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required=false) String departmentId, @RequestParam(required=false) String projectId,
        @RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="25") @Min(1) @Max(200) int size,
        @RequestParam(defaultValue="occurredAt,desc") String sort) {
        if (from != null && to != null && to.isBefore(from)) throw new IllegalArgumentException("to must not be before from");
        ReportingScopePolicy.Scope scope = scopePolicy.resolve(jwt, departmentId, projectId);
        return query.facts(eventType, from, to, scope.departmentId(), scope.projectId(),
            PageRequest.of(page, size, parseSort(sort, Set.of("occurredAt", "ingestedAt", "eventType", "entityType", "status"))));
    }

    private Sort parseSort(String raw, Set<String> allowedProperties) {
        String[] parts = raw.split(",", -1);
        String property = parts[0].trim();
        if (parts.length > 2 || !allowedProperties.contains(property))
            throw new IllegalArgumentException("Unsupported sort field: " + property);
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            if (parts[1].equalsIgnoreCase("asc")) direction = Sort.Direction.ASC;
            else if (!parts[1].equalsIgnoreCase("desc")) throw new IllegalArgumentException("Sort direction must be asc or desc");
        }
        return Sort.by(direction, property);
    }
}
