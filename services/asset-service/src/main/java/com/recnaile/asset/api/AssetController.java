package com.recnaile.asset.api;

import java.time.Clock;
import java.time.Instant;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.function.Supplier;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.recnaile.asset.api.dto.*;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.security.AssetAuthorizationService;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetLifecycleService;
import com.recnaile.asset.service.AssetReturnService;
import com.recnaile.asset.service.AssetService;
import com.recnaile.asset.idempotency.IdempotencyRecord;
import com.recnaile.asset.idempotency.IdempotencyService;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Reusable asset lifecycle and assignment operations")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService assetService;
    private final AssetAssignmentService assignmentService;
    private final AssetLifecycleService lifecycleService;
    private final AssetReturnService returnService;
    private final AssetAuthorizationService authorizationService;
    private final Clock clock;
    private final IdempotencyService idempotencyService;

    @Autowired
    public AssetController(
            AssetService assetService,
            AssetAssignmentService assignmentService,
            AssetLifecycleService lifecycleService,
            AssetReturnService returnService,
            AssetAuthorizationService authorizationService,
            Clock clock,
            IdempotencyService idempotencyService) {
        this.assetService = assetService;
        this.assignmentService = assignmentService;
        this.lifecycleService = lifecycleService;
        this.returnService = returnService;
        this.authorizationService = authorizationService;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
    }

    /** Test-friendly constructor retained for controller unit tests that do not need persistence-backed idempotency. */
    AssetController(
            AssetService assetService, AssetAssignmentService assignmentService, AssetLifecycleService lifecycleService,
            AssetReturnService returnService, AssetAuthorizationService authorizationService, Clock clock) {
        this(assetService, assignmentService, lifecycleService, returnService, authorizationService, clock, null);
    }

    @PostMapping
    @Operation(summary = "Create an asset", description = "Creates a reusable asset lifecycle record. Supports an optional Idempotency-Key header.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset created"),
            @ApiResponse(responseCode = "400", description = "Validation failure", content = @Content(schema = @Schema(implementation = com.recnaile.asset.exception.ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Idempotency or concurrency conflict")
    })
    public ResponseEntity<AssetResponse> create(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAssetRequest request) {
        authorizationService.requireAnyRole(authentication, "ADMIN", "MD", "PROCUREMENT");
        Supplier<Asset> action = () -> assetService.create(request.itemId(), request.issueReference(), request.name(), request.brand(),
                request.model(), request.issuedBy(), request.condition(), request.remarks(), Instant.now(clock));
        Asset asset = idempotencyService == null ? action.get() : idempotencyService.execute(
                idempotencyKey, "CREATE_ASSET",
                com.recnaile.asset.idempotency.IdempotencyService.fingerprint(
                        request.itemId(), request.issueReference(), request.name(), request.brand(), request.model(),
                        request.issuedBy(), request.condition(), request.remarks()), action, record -> assetService.get(record.getResourceId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponse.from(asset));
    }

    @GetMapping
    @Operation(summary = "List assets", description = "Lists assets, optionally filtered by assetId.")
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
    @Operation(summary = "Find asset by issue reference")
    public AssetResponse getByIssue(Authentication authentication, @PathVariable String issueReference) {
        Asset asset = assetService.getByIssueReference(issueReference);
        authorizationService.requireAssetRead(authentication, asset);
        return AssetResponse.from(asset);
    }

    /** Asset IDs contain '/', so asset-specific operations accept the ID as a query parameter. */
    @PostMapping("/assign")
    @Operation(summary = "Assign an asset", description = "Starts an assignment for an asset. Asset ID is a query parameter because the generated ID contains slashes.")
    public ResponseEntity<AssignmentResponse> assign(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AssignAssetRequest request) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        Supplier<com.recnaile.asset.model.AssetAssignment> action = () -> assignmentService.startAssignment(
                assetId, request.employeeId(), request.departmentName(), request.issuedBy(), request.outwardId(),
                Instant.now(clock), request.remarks());
        var assignment = idempotencyService == null ? action.get() : idempotencyService.execute(
                idempotencyKey, "ASSIGN_ASSET",
                com.recnaile.asset.idempotency.IdempotencyService.fingerprint(assetId, request.employeeId(),
                        request.departmentName(), request.issuedBy(), request.outwardId(), request.remarks()),
                action, record -> assignmentService.getById(record.getResourceId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AssignmentResponse.from(assignment));
    }

    @PostMapping("/return")
    @Operation(summary = "Return an asset", description = "Closes the current assignment and applies the return condition.")
    public AssignmentResponse returnAsset(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnAssetRequest request) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        Supplier<com.recnaile.asset.model.AssetAssignment> action = () -> returnService.processReturn(
                assetId, request.returnId(), request.condition(), Instant.now(clock));
        var assignment = idempotencyService == null ? action.get() : idempotencyService.execute(
                idempotencyKey, "RETURN_ASSET",
                com.recnaile.asset.idempotency.IdempotencyService.fingerprint(assetId, request.returnId(), request.condition()),
                action, record -> assignmentService.getById(record.getResourceId()));
        return AssignmentResponse.from(assignment);
    }

    @PostMapping("/repair/complete")
    @Operation(summary = "Complete asset repair")
    public AssetResponse completeRepair(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        Supplier<Asset> action = () -> lifecycleService.completeRepair(assetId, Instant.now(clock));
        var assetResult = idempotencyService == null ? action.get() : idempotencyService.execute(
                idempotencyKey, "COMPLETE_REPAIR",
                com.recnaile.asset.idempotency.IdempotencyService.fingerprint(assetId), action,
                record -> assetService.get(record.getResourceId()));
        return AssetResponse.from(assetResult);
    }

    @PostMapping("/retire")
    @Operation(summary = "Retire an asset")
    public AssetResponse retire(
            Authentication authentication,
            @RequestParam String assetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetMutation(authentication, asset);
        Supplier<Asset> action = () -> lifecycleService.retire(assetId, Instant.now(clock));
        var assetResult = idempotencyService == null ? action.get() : idempotencyService.execute(
                idempotencyKey, "RETIRE_ASSET",
                com.recnaile.asset.idempotency.IdempotencyService.fingerprint(assetId), action,
                record -> assetService.get(record.getResourceId()));
        return AssetResponse.from(assetResult);
    }

    @GetMapping("/history")
    @Operation(summary = "Get assignment history for an asset")
    public List<AssignmentResponse> history(Authentication authentication, @RequestParam String assetId) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetRead(authentication, asset);
        return assignmentService.historyForAsset(assetId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/employee/{employeeId}")
    @Operation(summary = "Get assignment history for an employee")
    public List<AssignmentResponse> historyForEmployee(Authentication authentication, @PathVariable String employeeId) {
        authorizationService.requireEmployeeHistory(authentication, employeeId);
        return assignmentService.historyForEmployee(employeeId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/department/{departmentName}")
    @Operation(summary = "Get assignment history for a department")
    public List<AssignmentResponse> historyForDepartment(Authentication authentication, @PathVariable String departmentName) {
        authorizationService.requireDepartmentHistory(authentication, departmentName);
        return assignmentService.historyForDepartment(departmentName).stream().map(AssignmentResponse::from).toList();
    }
}
