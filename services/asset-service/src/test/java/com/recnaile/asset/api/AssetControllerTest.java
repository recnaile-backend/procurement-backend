package com.recnaile.asset.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recnaile.asset.api.dto.CreateAssetRequest;
import com.recnaile.asset.api.dto.AssetGroupResponse;
import com.recnaile.asset.model.Asset;
import com.recnaile.asset.model.AssetCondition;
import com.recnaile.asset.model.AssetId;
import com.recnaile.asset.model.AssetStatus;
import com.recnaile.asset.security.AssetAuthorizationService;
import com.recnaile.asset.service.AssetAssignmentService;
import com.recnaile.asset.service.AssetLifecycleService;
import com.recnaile.asset.service.AssetService;
import com.recnaile.asset.service.AssetReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    @Mock AssetService assetService;
    @Mock AssetAssignmentService assignmentService;
    @Mock AssetLifecycleService lifecycleService;
    @Mock AssetReturnService returnService;
    @Mock AssetAuthorizationService authorizationService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AssetController(
                assetService, assignmentService, lifecycleService, returnService, authorizationService,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)))
                .setControllerAdvice(new com.recnaile.asset.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201() throws Exception {
        Asset asset = Asset.create(new AssetId("ASSET%2F26-27%2F001"), "ITEM-1", "OUT-1", "Laptop", "Brand", "Model",
                "issuer-1", AssetStatus.RETURNED, AssetCondition.GOOD, null, Instant.parse("2026-01-01T00:00:00Z"));
        when(assetService.create(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(asset);

        CreateAssetRequest request = new CreateAssetRequest("ITEM-1", "OUT-1", "Laptop", "Brand", "Model",
                "issuer-1", AssetCondition.GOOD, null);

        mockMvc.perform(post("/api/v1/assets")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidCreateReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/assets")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllReturns200() throws Exception {
        when(assetService.getAll(null)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk());
    }

    @Test
    void getGroupsReturns200() throws Exception {
        Asset asset = Asset.create(new AssetId("ASSET%2F26-27%2F001"), "Item-3", "OUT%2F26-27%2F008",
                "Item-3", null, null, "issuer-1", AssetStatus.IN_USE, AssetCondition.GOOD, null,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(assetService.getAll(null)).thenReturn(List.of(asset));
        when(assetService.toGroups(any())).thenReturn(List.of(new AssetGroupResponse(
                "OUT%2F26-27%2F008", "Item-3", 1, 1, 0, 0, 0, List.of("EMP2"))));

        mockMvc.perform(get("/api/v1/assets/groups"))
                .andExpect(status().isOk());
        verify(assetService).getAll(null);
    }

    @Test
    void getAllWithAssetIdReturns200() throws Exception {
        Asset asset = Asset.create(new AssetId("ASSET%2F26-27%2F001"), "ITEM-1", "OUT-1", "Laptop", "Brand", "Model",
                "issuer-1", AssetStatus.RETURNED, AssetCondition.GOOD, null, Instant.parse("2026-01-01T00:00:00Z"));
        when(assetService.getAll(any(String.class))).thenReturn(List.of(asset));

        mockMvc.perform(get("/api/v1/assets").param("assetId", "ASSET%2F26-27%2F001"))
                .andExpect(status().isOk());
        verify(assetService).getAll(eq("ASSET%2F26-27%2F001"));
    }
}
