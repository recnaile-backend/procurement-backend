package com.recnaile.asset.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.recnaile.asset.api.dto.*;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetLifecycleService;
import com.recnaile.asset.service.AssetReturnService;
import com.recnaile.asset.service.AssetService;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;
    private final AssetAssignmentService assignmentService;
    private final AssetLifecycleService lifecycleService;
    private final AssetReturnService returnService;
    private final Clock clock;

    public AssetController(AssetService assetService, AssetAssignmentService assignmentService,
            AssetLifecycleService lifecycleService, AssetReturnService returnService, Clock clock) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
        this.lifecycleService = lifecycleService;
        this.returnService = returnService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAssetRequest request) {
        Asset asset = assetService.create(request.itemId(), request.issueReference(), request.name(), request.brand(),
                request.model(), request.issuedBy(), request.condition(), request.remarks(), Instant.now(clock));
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponse.from(asset));
    }

    @GetMapping
    public List<AssetResponse> getAll(@RequestParam(required = false) String assetId) {
        return assetService.getAll(assetId).stream().map(AssetResponse::from).toList();
    }

    @GetMapping("/by-issue/{issueReference}")
    public AssetResponse getByIssue(@PathVariable String issueReference) {
        return AssetResponse.from(assetService.getByIssueReference(issueReference));
    }

    /** Asset IDs contain '/', so asset-specific operations accept the ID as a query parameter. */
    @PostMapping("/assign")
    public ResponseEntity<AssignmentResponse> assign(
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AssignAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AssignmentResponse.from(assignmentService.startAssignment(
                assetId, request.employeeId(), request.departmentName(), request.issuedBy(), request.outwardId(),
                Instant.now(clock), request.remarks())));
    }

    @PostMapping("/return")
    public AssignmentResponse returnAsset(
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnAssetRequest request) {
        return AssignmentResponse.from(returnService.processReturn(
                assetId, request.returnId(), request.condition(), Instant.now(clock)));
    }

    @PostMapping("/repair/complete")
    public AssetResponse completeRepair(
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return AssetResponse.from(lifecycleService.completeRepair(assetId, Instant.now(clock)));
    }

    @PostMapping("/retire")
    public AssetResponse retire(
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return AssetResponse.from(lifecycleService.retire(assetId, Instant.now(clock)));
    }

    @GetMapping("/history")
    public List<AssignmentResponse> history(@RequestParam String assetId) {
        return assignmentService.historyForAsset(assetId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/employee/{employeeId}")
    public List<AssignmentResponse> historyForEmployee(@PathVariable String employeeId) {
        return assignmentService.historyForEmployee(employeeId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/department/{departmentName}")
    public List<AssignmentResponse> historyForDepartment(@PathVariable String departmentName) {
        return assignmentService.historyForDepartment(departmentName).stream().map(AssignmentResponse::from).toList();
    }
}
