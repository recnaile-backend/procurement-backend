package com.recnaile.returns.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.returns.dto.CreateReturnRequest;
import com.recnaile.returns.dto.VerifyReturnRequest;
import com.recnaile.returns.model.entity.ReturnRequest;
import com.recnaile.returns.model.enums.ReturnCondition;
import com.recnaile.returns.service.ReturnRequestService;
import com.recnaile.returns.service.ReturnProcessingService;
import com.recnaile.returns.service.ReturnVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReturnRequestController.class)
@AutoConfigureMockMvc
@Import(ReturnExceptionHandler.class)
class ReturnRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ReturnRequestService returnRequestService;
    @MockitoBean ReturnVerificationService returnVerificationService;
    @MockitoBean ReturnProcessingService returnProcessingService;

    @Test
    @WithMockUser(username = "user-1", roles = "EMPLOYEE")
    void createReturns201ForValidRequest() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                null, "user-1", ReturnCondition.GOOD, "return");
        ReturnRequest created = new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", ReturnCondition.GOOD, "return", null, Instant.parse("2026-09-30T05:00:00Z"));

        when(returnRequestService.create(any(CreateReturnRequest.class), eq("user-1"), eq(null))).thenReturn(created);

        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    @WithMockUser(username = "verifier-1", roles = "PROCUREMENT")
    void verifyReturns200ForValidRequest() throws Exception {
        VerifyReturnRequest request = new VerifyReturnRequest("verifier-1");
        ReturnRequest verified = new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", ReturnCondition.GOOD, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));
        verified.setStatus(com.recnaile.returns.model.enums.ReturnStatus.VERIFIED);
        verified.setVerifiedBy("verifier-1");
        verified.setVerifiedAt(Instant.parse("2026-09-30T05:30:00Z"));

        when(returnVerificationService.verify(eq("RET-1"), any(VerifyReturnRequest.class), eq("verifier-1"), eq("verify-1")))
                .thenReturn(verified);

        mockMvc.perform(post("/api/v1/returns/RET-1/verify")
                        .header("Idempotency-Key", "verify-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.verifiedBy").value("verifier-1"));
    }

    @Test
    @WithMockUser(username = "procurement-1", roles = "PROCUREMENT")
    void processReturns200ForGoodReturn() throws Exception {
        ReturnRequest processed = new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", ReturnCondition.GOOD, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));
        processed.setStatus(com.recnaile.returns.model.enums.ReturnStatus.ACCEPTED_GOOD);

        when(returnProcessingService.process(eq("RET-1"), eq("procurement-1"), eq("process-1"))).thenReturn(processed);

        mockMvc.perform(post("/api/v1/returns/RET-1/process")
                        .header("Idempotency-Key", "process-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED_GOOD"));
    }

    @Test
    @WithMockUser(username = "user-1", roles = "EMPLOYEE")
    void createWorksWithoutIdempotencyKey() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest(
                "OUT-101", "OUT-101-L1", "ITEM-11", new BigDecimal("1"),
                null, "user-1", ReturnCondition.GOOD, "return");
        ReturnRequest created = new ReturnRequest(
                "OUT-101", "OUT-101-L1", "ITEM-11", new BigDecimal("1"),
                "user-1", ReturnCondition.GOOD, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));

        when(returnRequestService.create(any(CreateReturnRequest.class), eq("user-1"), eq(null))).thenReturn(created);

        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "user-1", roles = "EMPLOYEE")
    void createRejectsMissingRequiredFields() throws Exception {
        String invalid = "{\"outwardId\":\"\",\"outwardLineId\":\"OUT-1-L1\",\"itemId\":\"ITEM-1\",\"quantity\":0,\"returnedBy\":\"user-1\",\"condition\":\"GOOD\"}";

        mockMvc.perform(post("/api/v1/returns")
                        .header("Idempotency-Key", "create-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "verifier-1", roles = "PROCUREMENT")
    void verifyWorksWithoutIdempotencyKey() throws Exception {
        VerifyReturnRequest request = new VerifyReturnRequest("verifier-1");
        ReturnRequest verified = new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", ReturnCondition.GOOD, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));
        verified.setStatus(com.recnaile.returns.model.enums.ReturnStatus.VERIFIED);

        when(returnVerificationService.verify(eq("RET-1"), any(VerifyReturnRequest.class),
                eq("verifier-1"), eq(null))).thenReturn(verified);

        mockMvc.perform(post("/api/v1/returns/RET-1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "procurement-1", roles = "PROCUREMENT")
    void processWorksWithoutIdempotencyKey() throws Exception {
        ReturnRequest processed = new ReturnRequest(
                "OUT-100", "OUT-100-L1", "ITEM-10", new BigDecimal("2"),
                "user-1", ReturnCondition.GOOD, "return", null,
                Instant.parse("2026-09-30T05:00:00Z"));
        processed.setStatus(com.recnaile.returns.model.enums.ReturnStatus.ACCEPTED_GOOD);

        when(returnProcessingService.process(eq("RET-1"), eq("procurement-1"), eq(null)))
                .thenReturn(processed);

        mockMvc.perform(post("/api/v1/returns/RET-1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

}
