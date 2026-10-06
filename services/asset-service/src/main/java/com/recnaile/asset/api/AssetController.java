package com.recnaile.asset.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to create assets"),
            @ApiResponse(responseCode = "409", description = "Idempotency or concurrency conflict"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<AssetResponse> create(
            Authentication authentication,
            @Parameter(description = "Optional idempotency key for safe retries", example = "asset-create-20261006-001")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assets returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to read the requested assets"),
            @ApiResponse(responseCode = "404", description = "Requested asset was not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public List<AssetResponse> getAll(Authentication authentication, @Parameter(description = "Optional asset ID filter", example = "ASSET%2F26-27%2F001")
            @RequestParam(required = false) String assetId) {
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
    @Operation(summary = "Find asset by issue reference", description = "Returns the reusable asset associated with the supplied outward issue reference.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to read the asset"),
            @ApiResponse(responseCode = "404", description = "Asset was not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public AssetResponse getByIssue(Authentication authentication, @Parameter(description = "Outward issue reference", example = "OUT%2F26-27%2F001")
            @PathVariable String issueReference) {
        Asset asset = assetService.getByIssueReference(issueReference);
        authorizationService.requireAssetRead(authentication, asset);
        return AssetResponse.from(asset);
    }

    /** Asset IDs contain '/', so asset-specific operations accept the ID as a query parameter. */
    @PostMapping("/assign")
    @Operation(summary = "Assign an asset", description = "Starts an assignment for an asset. Asset ID is a query parameter because the generated ID contains slashes.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to assign this asset"),
            @ApiResponse(responseCode = "404", description = "Asset was not found"),
            @ApiResponse(responseCode = "409", description = "Asset state, assignment, idempotency or concurrency conflict"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<AssignmentResponse> assign(
            Authentication authentication,
            @Parameter(description = "Asset identifier", example = "ASSET%2F26-27%2F001")
            @RequestParam String assetId,
            @Parameter(description = "Optional idempotency key for safe retries", example = "asset-action-20261006-001")
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset return recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failure"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to return this asset"),
            @ApiResponse(responseCode = "404", description = "Asset or assignment was not found"),
            @ApiResponse(responseCode = "409", description = "Asset state, idempotency or concurrency conflict"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public AssignmentResponse returnAsset(
            Authentication authentication,
            @Parameter(description = "Asset identifier", example = "ASSET%2F26-27%2F001")
            @RequestParam String assetId,
            @Parameter(description = "Optional idempotency key for safe retries", example = "asset-action-20261006-001")
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
    @Operation(summary = "Complete asset repair", description = "Marks a repair workflow as completed and transitions the asset to its next valid lifecycle state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repair completed"),
            @ApiResponse(responseCode = "400", description = "Invalid asset identifier or request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to modify this asset"),
            @ApiResponse(responseCode = "404", description = "Asset was not found"),
            @ApiResponse(responseCode = "409", description = "Asset state, idempotency or concurrency conflict"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public AssetResponse completeRepair(
            Authentication authentication,
            @Parameter(description = "Asset identifier", example = "ASSET%2F26-27%2F001")
            @RequestParam String assetId,
            @Parameter(description = "Optional idempotency key for safe retries", example = "asset-action-20261006-001")
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
    @Operation(summary = "Retire an asset", description = "Retires an asset and prevents further normal assignment operations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset retired"),
            @ApiResponse(responseCode = "400", description = "Invalid asset identifier or request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to modify this asset"),
            @ApiResponse(responseCode = "404", description = "Asset was not found"),
            @ApiResponse(responseCode = "409", description = "Asset state, idempotency or concurrency conflict"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public AssetResponse retire(
            Authentication authentication,
            @Parameter(description = "Asset identifier", example = "ASSET%2F26-27%2F001")
            @RequestParam String assetId,
            @Parameter(description = "Optional idempotency key for safe retries", example = "asset-action-20261006-001")
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
    @Operation(summary = "Get assignment history for an asset", description = "Returns the complete assignment history for the requested asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment history returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to view this asset history"),
            @ApiResponse(responseCode = "404", description = "Asset was not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public List<AssignmentResponse> history(Authentication authentication, @Parameter(description = "Asset identifier", example = "ASSET%2F26-27%2F001")
            @RequestParam String assetId) {
        Asset asset = assetService.get(assetId);
        authorizationService.requireAssetRead(authentication, asset);
        return assignmentService.historyForAsset(assetId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/employee/{employeeId}")
    @Operation(summary = "Get assignment history for an employee", description = "Returns assignment history visible to the authenticated user for the supplied employee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment history returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to view this employee history"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public List<AssignmentResponse> historyForEmployee(Authentication authentication, @Parameter(description = "Employee identifier", example = "EMP-001")
            @PathVariable String employeeId) {
        authorizationService.requireEmployeeHistory(authentication, employeeId);
        return assignmentService.historyForEmployee(employeeId).stream().map(AssignmentResponse::from).toList();
    }

    @GetMapping("/history/department/{departmentName}")
    @Operation(summary = "Get assignment history for a department", description = "Returns assignment history visible to the authenticated user for the supplied department.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment history returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not allowed to view this department history"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public List<AssignmentResponse> historyForDepartment(Authentication authentication, @Parameter(description = "Department name", example = "Engineering")
            @PathVariable String departmentName) {
        authorizationService.requireDepartmentHistory(authentication, departmentName);
        return assignmentService.historyForDepartment(departmentName).stream().map(AssignmentResponse::from).toList();
    }
}
