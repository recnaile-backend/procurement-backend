package com.recnaile.inventory.controller;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.service.InventoryQueryService;
import com.recnaile.inventory.service.ReconciliationService;
import com.recnaile.inventory.service.ThresholdService;

class InventoryControllerTest {

    @Mock
    InventoryQueryService query;

    @Mock
    ThresholdService threshold;

    @Mock
    ReconciliationService reconciliation;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mvc = MockMvcBuilders
                .standaloneSetup(
                        new InventoryController(
                                query,
                                threshold,
                                reconciliation))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getStockReturnsBalances() throws Exception {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(10);
        balance.setDefectQuantity(1);
        balance.setStatus(StockStatus.IN_STOCK);

        when(query.byItem("ITEM-1"))
                .thenReturn(List.of(balance));

        mvc.perform(
                get("/api/v1/inventory/stock/ITEM-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemId", is("ITEM-1")))
                .andExpect(jsonPath("$[0].availableQuantity", is(10.0)));
    }

    @Test
    void getSummaryReturnsSummary() throws Exception {
        when(query.summary("ITEM-1"))
                .thenReturn(
                        new InventorySummaryResponse(
                                "ITEM-1",
                                10,
                                1,
                                11,
                                5,
                                50,
                                false,
                                StockStatus.IN_STOCK));

        mvc.perform(
                get("/api/v1/inventory/stock/ITEM-1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.availableQuantity", is(10.0)))
                .andExpect(jsonPath("$.status", is("IN_STOCK")));
    }

    @Test
    void getMovementsPassesPaginationToService() throws Exception {
        PageRequest requestedPage = PageRequest.of(0, 5);

        when(query.movementsByItem(
                eq("ITEM-1"),
                any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(),
                                requestedPage,
                                0));

        mvc.perform(
                get("/api/v1/inventory/stock/ITEM-1/movements")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(0)));

        verify(query).movementsByItem(
                eq("ITEM-1"),
                any(Pageable.class));
    }

    @Test
    void updateThresholdsReturnsUpdatedBalance() throws Exception {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setMinimumStock(10);
        balance.setMaximumStock(50);

        when(threshold.update(
                "ITEM-1",
                "DEPT-1",
                "PROJ-1",
                "RACK-A",
                10,
                50))
                .thenReturn(balance);

        mvc.perform(
                put("/api/v1/inventory/stock/ITEM-1/thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": "DEPT-1",
                                  "projectId": "PROJ-1",
                                  "location": "RACK-A",
                                  "minimumStock": 10,
                                  "maximumStock": 50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.minimumStock", is(10.0)))
                .andExpect(jsonPath("$.maximumStock", is(50.0)));
    }
}
