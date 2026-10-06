package com.recnaile.asset.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.recnaile.asset.api.dto.*;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.security.AssetAuthorizationService;
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
    private final AssetAuthorizationService authorizationService;
    private final Clock clock;

    public AssetController(
            AssetService assetService,
            AssetAssignmentService assignmentService,
            AssetLifecycleService lifecycleService,
            AssetReturnService returnService,
            AssetAuthorizationService authorizationService,
            Clock clock) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
        this.lifecycleService = lifecycleService;
        this.returnService = returnService;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAssetRequest request) {
        authorizationService.requireAnyRole(authentication, "ADMIN", "MD", "PROCUREMENT");
        Asset asset = assetService.create(request.itemId(), request.issueReference(), request.name(), request.brand(),
                request.model(), request.issuedBy(), request.condition(), request.remarks(), Instant.now(clock));
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponse.from(asset));
    }

    @GetMapping
    public List<AssetResponse> getAll(Authentication authentication, @RequestParam(required = false) String assetId) {
        List<Asset> assets = assetService.getAll(assetId);
        if (assetId != null && !assetId.isBlank()) {
            Asset asset = assets.get(0);
            authorizationService.requireAssetRead(authentication, asset);
            return List.of(AssetResponse.from(asset));
        }
        return authorizationService.filterReadableAssets(authentication, assets)
                .stream().map(AssetResponse::from).toList();
    }

    @GetMapping("/by-issue/{issueReference}")
    public AssetResponse getByIssue(Authentication authentication, @PathVariable String issueReference) {
        Asset asset = assetService.getByIssueReference(issueReference);
        authorizationService.requireAssetRead(authentication, asset);
        return AssetResponse.from(asset);
    }

    /** Asset IDs contain '/', so asset-specific operations accept the ID as a query parameter. */
    @PostMapping("/assign")
    public ResponseEntity<AssignmentResponse> assign(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AssignAssetRequest request) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(AssignmentResponse.from(assignmentService.startAssignment(
                assetId, request.employeeId(), request.departmentName(), request.issuedBy(), request.outwardId(),
                Instant.now(clock), request.remarks())));
    }

    @PostMapping("/return")
    public AssignmentResponse returnAsset(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnAssetRequest request) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        return AssignmentResponse.from(returnService.processReturn(
                assetId, request.returnId(), request.condition(), Instant.now(clock)));
    }

    @PostMapping("/repair/complete")
    public AssetResponse completeRepair(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        return AssetResponse.from(lifecycleService.completeRepair(assetId, Instant.now(clock)));
    }

    @PostMapping("/retire")
    public AssetResponse retire(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        return AssetResponse.from(lifecycleService.retire(assetId, Instant.now(clock)));
    }

    @GetMapping("/history")
    public List<AssignmentResponse> history(Authentication authentication, @RequestParam String assetId) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetRead(authentication, asset);
        return assignmentService.historyForAsset(assetId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/employee/{employeeId}")
    public List<AssignmentResponse> historyForEmployee(Authentication authentication, @PathVariable String employeeId) {
        authorizationService.requireEmployeeHistory(authentication, employeeId);
        return assignmentService.historyForEmployee(employeeId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/department/{departmentName}")
    public List<AssignmentResponse> historyForDepartment(Authentication authentication, @PathVariable String departmentName) {
        authorizationService.requireDepartmentHistory(authentication, departmentName);
        return assignmentService.historyForDepartment(departmentName).stream().map(AssignmentResponse::from).toList();
    }
}
