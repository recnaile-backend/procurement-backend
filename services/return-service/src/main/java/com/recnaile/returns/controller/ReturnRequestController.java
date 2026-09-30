package com.recnaile.returns.controller;

import com.recnaile.returns.api.ApiResponse;
import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.service.ReturnRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnRequest>> create(@Valid @RequestBody CreateReturnRequest request) {
        ReturnRequest created = returnRequestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }
}
