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
import com.recnaile.returns.security.SecurityConfig;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.net.URI;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReturnRequestController.class)
@AutoConfigureMockMvc
@Import({ReturnExceptionHandler.class, SecurityConfig.class})
class ReturnRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean ReturnRequestService returnRequestService;
    @MockitoBean ReturnVerificationService returnVerificationService;
    @MockitoBean ReturnProcessingService returnProcessingService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(username = "tl-1", roles = "TL")
    void getAllReturns200ForTl() throws Exception {
        when(returnRequestService.getAll(null)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/returns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }


    @Test
    @WithMockUser(username = "md-1", roles = "MD")
    void getAllReturns200ForMdWithOptionalReturnId() throws Exception {
        ReturnRequest result = new ReturnRequest(
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return",
                Instant.parse("2026-09-30T05:00:00Z"));
        result.setReturnId("RET-1");
        when(returnRequestService.getAll("RET-1")).thenReturn(java.util.List.of(result));

        mockMvc.perform(get("/api/v1/returns").param("returnId", "RET-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].returnId").value("RET-1"));
    }

    @Test
    @WithMockUser(username = "employee-1", roles = "EMPLOYEE")
    void getAllReturns403ForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/returns"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-1", roles = "EMPLOYEE")
    void createReturns201ForValidRequest() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest(
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return");
        ReturnRequest created = new ReturnRequest(
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return", Instant.parse("2026-09-30T05:00:00Z"));

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
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return",
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
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return",
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
    void createUsesEmployeeIdClaimFromJwt() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest(
                "OUT-102", "ITEM-12", 1,
                "EMP001", ReturnCondition.GOOD, "return");
        ReturnRequest created = new ReturnRequest(
                "OUT-102", "ITEM-12", 1,
                "EMP001", ReturnCondition.GOOD, "return",
                Instant.parse("2026-09-30T05:00:00Z"));

        when(returnRequestService.create(any(CreateReturnRequest.class), eq("EMP001"), eq(null)))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/returns")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("6aaa1e501b3bf461314b8e2a")
                                .claim("employeeId", "EMP001")
                                .claim("roles", List.of("EMPLOYEE")))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.returnByEmpId").value("EMP001"));
    }

    @Test
    @WithMockUser(username = "user-1", roles = "EMPLOYEE")
    void createWorksWithoutIdempotencyKey() throws Exception {
        CreateReturnRequest request = new CreateReturnRequest(
                "OUT-101", "ITEM-11", 1,
                "user-1", ReturnCondition.GOOD, "return");
        ReturnRequest created = new ReturnRequest(
                "OUT-101", "ITEM-11", 1,
                "user-1", ReturnCondition.GOOD, "return",
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
        String invalid = "{\"outwardId\":\"\",\"itemId\":\"ITEM-1\",\"quantity\":0,\"returnByEmpId\":\"user-1\",\"condition\":\"GOOD\"}";

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
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return",
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
                "OUT-100", "ITEM-10", 2,
                "user-1", ReturnCondition.GOOD, "return",
                Instant.parse("2026-09-30T05:00:00Z"));
        processed.setStatus(com.recnaile.returns.model.enums.ReturnStatus.ACCEPTED_GOOD);

        when(returnProcessingService.process(eq("RET-1"), eq("procurement-1"), eq(null)))
                .thenReturn(processed);

        mockMvc.perform(post("/api/v1/returns/RET-1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyUsesEmployeeIdClaimFromJwt() throws Exception {
        VerifyReturnRequest request = new VerifyReturnRequest("EMP002");
        ReturnRequest verified = new ReturnRequest(
                "OUT-100", "ITEM-10", 2,
                "EMP001", ReturnCondition.GOOD, "return",
                Instant.parse("2026-09-30T05:00:00Z"));
        verified.setStatus(com.recnaile.returns.model.enums.ReturnStatus.VERIFIED);
        verified.setVerifiedBy("EMP002");
        verified.setVerifiedAt(Instant.parse("2026-09-30T05:30:00Z"));

        when(returnVerificationService.verify(eq("RET-1"), any(VerifyReturnRequest.class), eq("EMP002"), eq(null)))
                .thenReturn(verified);

        mockMvc.perform(post("/api/v1/returns/RET-1/verify")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("auth-user-id")
                                .claim("employeeId", "EMP002")
                                .claim("roles", List.of("PROCUREMENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PROCUREMENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verifiedBy").value("EMP002"));
    }

    @Test
    void processUsesEmployeeIdClaimFromJwt() throws Exception {
        ReturnRequest processed = new ReturnRequest(
                "OUT-100", "ITEM-10", 2,
                "EMP001", ReturnCondition.GOOD, "return",
                Instant.parse("2026-09-30T05:00:00Z"));
        processed.setStatus(com.recnaile.returns.model.enums.ReturnStatus.ACCEPTED_GOOD);

        when(returnProcessingService.process(eq("RET-1"), eq("EMP002"), eq(null))).thenReturn(processed);

        mockMvc.perform(post("/api/v1/returns/RET-1/process")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("auth-user-id")
                                .claim("employeeId", "EMP002")
                                .claim("roles", List.of("PROCUREMENT")))
                                .authorities(new SimpleGrantedAuthority("ROLE_PROCUREMENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED_GOOD"));
    }



    @Test
    void verifyAcceptsSwaggerDoubleEncodedReturnIdWithJwt() throws Exception {
        VerifyReturnRequest request = new VerifyReturnRequest("EMP001");
        ReturnRequest verified = new ReturnRequest(
                "OUT%2F26-27%2F001", "Item-2", 2,
                "EMP001", ReturnCondition.GOOD, "Work Done",
                Instant.parse("2026-10-05T05:00:00Z"));
        verified.setReturnId("RETURN%2F26-27%2F001");
        verified.setStatus(com.recnaile.returns.model.enums.ReturnStatus.VERIFIED);
        verified.setVerifiedBy("EMP001");

        when(returnVerificationService.verify(
                eq("RETURN%2F26-27%2F001"),
                any(VerifyReturnRequest.class),
                eq("EMP001"),
                eq(null))).thenReturn(verified);

        mockMvc.perform(post(URI.create("http://localhost/api/v1/returns/RETURN%252F26-27%252F001/verify"))
                        .with(jwt().jwt(jwt -> jwt
                                .subject("auth-user-id")
                                .claim("employeeId", "EMP001")
                                .claim("roles", List.of("MD")))
                                .authorities(new SimpleGrantedAuthority("ROLE_MD")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.returnId").value("RETURN%2F26-27%2F001"));

        org.mockito.Mockito.verify(returnVerificationService).verify(
                eq("RETURN%2F26-27%2F001"),
                any(VerifyReturnRequest.class),
                eq("EMP001"),
                eq(null));
    }

    @Test
    void processAcceptsSwaggerDoubleEncodedReturnIdWithJwt() throws Exception {
        ReturnRequest processed = new ReturnRequest(
                "OUT%2F26-27%2F001", "Item-2", 2,
                "EMP001", ReturnCondition.GOOD, "Work Done",
                Instant.parse("2026-10-05T05:00:00Z"));
        processed.setReturnId("RETURN%2F26-27%2F001");
        processed.setStatus(com.recnaile.returns.model.enums.ReturnStatus.ACCEPTED_GOOD);

        when(returnProcessingService.process(
                eq("RETURN%2F26-27%2F001"),
                eq("EMP001"),
                eq(null))).thenReturn(processed);

        mockMvc.perform(post(URI.create("http://localhost/api/v1/returns/RETURN%252F26-27%252F001/process"))
                        .with(jwt().jwt(jwt -> jwt
                                .subject("auth-user-id")
                                .claim("employeeId", "EMP001")
                                .claim("roles", List.of("MD")))
                                .authorities(new SimpleGrantedAuthority("ROLE_MD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.returnId").value("RETURN%2F26-27%2F001"));

        org.mockito.Mockito.verify(returnProcessingService).process(
                eq("RETURN%2F26-27%2F001"),
                eq("EMP001"),
                eq(null));
    }

}
