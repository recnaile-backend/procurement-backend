package com.recnaile.masterdata.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recnaile.masterdata.dto.item.CreateItemRequest;
import com.recnaile.masterdata.dto.item.ItemResponse;
import com.recnaile.masterdata.dto.item.UpdateItemRequest;
import com.recnaile.masterdata.service.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/master-data/items")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "Create item")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Item created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid item data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PostMapping
    public ResponseEntity<ItemResponse> create(
            @Valid @RequestBody CreateItemRequest request) {

        ItemResponse response = itemService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Multi-add items")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Items created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid items data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have the required role")
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PostMapping("/multi-add")
    public ResponseEntity<List<ItemResponse>> multiAdd(
            @Valid @RequestBody List<@Valid CreateItemRequest> requests) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(itemService.multiAdd(requests));
    }

    @Operation(summary = "Get all items")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Items retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAll() {

        return ResponseEntity.ok(
                itemService.getAll()
        );
    }

    @Operation(summary = "Get item by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item not found"
            )
    })
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getById(
            @PathVariable String itemId) {

        return ResponseEntity.ok(
                itemService.getById(itemId)
        );
    }

    @Operation(summary = "Update item")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid item data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Item version conflict"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponse> update(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {

        return ResponseEntity.ok(
                itemService.update(itemId, request)
        );
    }

    @Operation(summary = "Deactivate item")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item deactivated successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item not found"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @PostMapping("/{itemId}/deactivate")
    public ResponseEntity<ItemResponse> deactivate(
            @PathVariable String itemId) {

        return ResponseEntity.ok(
                itemService.deactivate(itemId)
        );
    }

    @Operation(summary = "Delete item by ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Item deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have the required role"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Item not found"
            )
    })
    @PreAuthorize("hasAnyRole('PROCUREMENT', 'MD')")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @PathVariable String itemId) {

        itemService.delete(itemId);

        return ResponseEntity.noContent().build();
    }
}