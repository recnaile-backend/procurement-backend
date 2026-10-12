package com.recnaile.reporting.web;

import com.recnaile.reporting.model.*;
import com.recnaile.reporting.service.ReportingQueryService;
import com.recnaile.reporting.service.ReportingScopePolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.recnaile.reporting.repository.ReportingSnapshotRepository;
import com.recnaile.reporting.model.ReportingSnapshot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reporting")
public class ReportingController {
    private final ReportingQueryService query; private final ReportingSnapshotRepository snapshots; private final ReportingScopePolicy scopePolicy;
    public ReportingController(ReportingQueryService query, ReportingSnapshotRepository snapshots, ReportingScopePolicy scopePolicy){this.query=query;this.snapshots=snapshots;this.scopePolicy=scopePolicy;}
    @GetMapping("/dashboard/summary") public Map<String,Object> dashboard(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String departmentId,@RequestParam(required=false) String projectId){ReportingScopePolicy.Scope scope=scopePolicy.resolve(jwt,departmentId,projectId);return query.dashboard(scope.departmentId(),scope.projectId());}
    @GetMapping("/dashboard/freshness") public ReportFreshness freshness(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String departmentId,@RequestParam(required=false) String projectId){ReportingScopePolicy.Scope scope=scopePolicy.resolve(jwt,departmentId,projectId);return query.freshness(scope.departmentId(),scope.projectId());}
    @GetMapping("/reports/snapshots") public Page<ReportingSnapshot> snapshots(@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="25") @Min(1) @Max(200) int size,@RequestParam(defaultValue="snapshotDate,desc") String sort){return snapshots.findAll(PageRequest.of(page,size,parseSort(sort)));}
    @GetMapping("/reports/period-summary") public Map<String,Object> period(@RequestParam ReportPeriod period,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return query.periodSummary(period,from,to);}
    @GetMapping("/reports/projections") public Page<ReportingProjection> projections(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String entityType,@RequestParam(required=false) String status,@RequestParam(required=false) String departmentId,@RequestParam(required=false) String projectId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="25") @Min(1) @Max(200) int size,@RequestParam(defaultValue="updatedAt,desc") String sort){
        ReportingScopePolicy.Scope scope=scopePolicy.resolve(jwt,departmentId,projectId);
        return query.projections(entityType,status,scope.departmentId(),scope.projectId(),PageRequest.of(page,size,parseSort(sort)));
    }
    @GetMapping("/reports/events") public Page<ReportingFact> events(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String eventType,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,@RequestParam(required=false) String departmentId,@RequestParam(required=false) String projectId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="25") @Min(1) @Max(200) int size,@RequestParam(defaultValue="occurredAt,desc") String sort){
        if(from!=null&&to!=null&&to.isBefore(from))throw new IllegalArgumentException("to must not be before from");
        ReportingScopePolicy.Scope scope=scopePolicy.resolve(jwt,departmentId,projectId);
        return query.facts(eventType,from,to,scope.departmentId(),scope.projectId(),PageRequest.of(page,size,parseSort(sort)));
    }
    private Sort parseSort(String raw){String[] parts=raw.split(",",2);String property=parts[0].trim(); if(!java.util.Set.of("updatedAt","createdAt","occurredAt","ingestedAt","entityType","status","eventType","snapshotDate","capturedAt").contains(property))throw new IllegalArgumentException("Unsupported sort field: "+property); Sort.Direction direction=parts.length>1&&parts[1].equalsIgnoreCase("asc")?Sort.Direction.ASC:Sort.Direction.DESC;return Sort.by(direction,property);}
}
