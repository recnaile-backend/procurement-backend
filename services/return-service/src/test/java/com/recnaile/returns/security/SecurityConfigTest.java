package com.recnaile.returns.security;

import com.recnaile.returns.controller.ReturnRequestController;
import com.recnaile.returns.controller.ReturnExceptionHandler;
import com.recnaile.returns.service.ReturnProcessingService;
import com.recnaile.returns.service.ReturnRequestService;
import com.recnaile.returns.service.ReturnVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReturnRequestController.class)
@Import({SecurityConfig.class, ReturnExceptionHandler.class})
class SecurityConfigTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ReturnRequestService returnRequestService;
    @MockitoBean ReturnVerificationService returnVerificationService;
    @MockitoBean ReturnProcessingService returnProcessingService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedGetAllReturnsIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/returns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "TL")
    void tlCanGetAllReturns() throws Exception {
        mockMvc.perform(get("/api/v1/returns"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "EMPLOYEE")
    void employeeCannotGetAllReturns() throws Exception {
        mockMvc.perform(get("/api/v1/returns"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/returns")
                        .header("Idempotency-Key", "security-1"))
                .andExpect(status().isUnauthorized());
    }
}
