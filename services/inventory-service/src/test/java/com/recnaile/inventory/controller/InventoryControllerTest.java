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

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.dto.InventorySummaryResponse;
import com.recnaile.inventory.service.InventoryAdjustmentService;
import com.recnaile.inventory.dto.InventoryReconciliationResponse;
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

    @Mock
    InventoryAdjustmentService adjustment;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mvc = MockMvcBuilders
                .standaloneSetup(
                        new InventoryController(
                                query,
                                threshold,
                                reconciliation,
                                adjustment))
                .build();
    }

    @Test
    void getAllStocksReturnsAllBalances() throws Exception {
        StockBalanceDocument first = new StockBalanceDocument();
        first.setItemId("ITEM-1");
        first.setAvailableQuantity(10);
        first.setDefectQuantity(1);

        StockBalanceDocument second = new StockBalanceDocument();
        second.setItemId("ITEM-2");
        second.setAvailableQuantity(25);
        second.setDefectQuantity(0);

        when(query.allStocks())
                .thenReturn(List.of(first, second));

        mvc.perform(get("/api/v1/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].itemId", is("ITEM-1")))
                .andExpect(jsonPath("$[1].itemId", is("ITEM-2")));

        verify(query).allStocks();
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
                .andExpect(jsonPath("$[0].availableQuantity", is(10)));
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
                .andExpect(jsonPath("$.availableQuantity", is(10)))
                .andExpect(jsonPath("$.status", is("IN_STOCK")));
    }

    @Test
    void getMovementsReturnsAllMovementsWithoutPagination() throws Exception {
        StockMovementDocument movement = new StockMovementDocument(
                "movement-1",
                "event-1",
                "GOODS_RECEIVED",
                "GRN-1",
                "ITEM-1",
                null,
                null,
                null,
                com.recnaile.inventory.domain.StockType.AVAILABLE,
                10,
                10,
                "GOODS_RECEIVED",
                java.time.Instant.parse("2026-09-22T12:00:00Z"),
                java.time.Instant.parse("2026-09-22T12:00:00Z"),
                "correlation-1",
                "trace-1",
                "test-user");

        when(query.movementsByItem("ITEM-1"))
                .thenReturn(List.of(movement));

        mvc.perform(
                get("/api/v1/inventory/stock/ITEM-1/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemId", is("ITEM-1")))
                .andExpect(jsonPath("$[0].quantity", is(10)))
                .andExpect(jsonPath("$[0].delta", is(10)));

        verify(query).movementsByItem("ITEM-1");
    }

    @Test
    void reconciliationReturnsStructuredResult() throws Exception {
        when(reconciliation.reconcile("ITEM-1"))
                .thenReturn(new InventoryReconciliationResponse("ITEM-1", 8, 8, 0, 2, 2, 0, true, true, true));

        mvc.perform(get("/api/v1/inventory/reconciliation/ITEM-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.balanceAvailable", is(8)))
                .andExpect(jsonPath("$.movementAvailable", is(8)))
                .andExpect(jsonPath("$.availableDifference", is(0)))
                .andExpect(jsonPath("$.reconciled", is(true)));
    }

    @Test
    void updateAvailableStockReturnsUpdatedBalance() throws Exception {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(50);

        when(adjustment.updateAvailableStock(
                "ITEM-1", "IT", "PROJ-1", "RACK-A", 50, "Physical stock count", "test-user"))
                .thenReturn(balance);

        mvc.perform(
                put("/api/v1/inventory/stock/ITEM-1/available-quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .principal(new TestingAuthenticationToken("test-user", null))
                        .content("""
                                {
                                  "availableQuantity": 50,
                                  "departmentName": "IT",
                                  "projectId": "PROJ-1",
                                  "location": "RACK-A",
                                  "reason": "Physical stock count"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.availableQuantity", is(50)));
    }

    @Test
    void updateThresholdsReturnsUpdatedBalance() throws Exception {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setMinimumStock(10);
        balance.setMaximumStock(50);

        when(threshold.update(
                "ITEM-1",
                "IT",
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
                                  "departmentName": "IT",
                                  "projectId": "PROJ-1",
                                  "location": "RACK-A",
                                  "minimumStock": 10,
                                  "maximumStock": 50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.minimumStock", is(10)))
                .andExpect(jsonPath("$.maximumStock", is(50)));
    }
}
