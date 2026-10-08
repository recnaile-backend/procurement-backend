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
        when(query.allStocks())
                .thenReturn(List.of(
                        stock("ITEM-1", 10, 1),
                        stock("ITEM-2", 25, 0)));

        mvc.perform(get("/api/v1/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].itemId", is("ITEM-1")))
                .andExpect(jsonPath("$[1].itemId", is("ITEM-2")));

        verify(query).allStocks();
    }

    @Test
    void getStockReturnsBalances() throws Exception {
        when(query.byItem("ITEM-1"))
                .thenReturn(List.of(stock("ITEM-1", 10, 1)));

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
    void updateThresholdsReturnsProjectedBalance() throws Exception {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setBalanceKey("ITEM-1|IT|PROJ-1|RACK-A");
        balance.setDepartmentName("IT");
        balance.setProjectId("PROJ-1");
        balance.setLocation("RACK-A");
        balance.setMinimumStock(10);
        balance.setMaximumStock(50);
        balance.setThresholdsOverridden(true);

        when(threshold.update("ITEM-1", "IT", "PROJ-1", "RACK-A", 10, 50, "test-user"))
                .thenReturn(balance);

        when(query.byItem("ITEM-1")).thenReturn(List.of(
                new com.recnaile.inventory.dto.InventoryStockResponse(
                        "ITEM-1|IT|PROJ-1|RACK-A", "ITEM-1", "Test item", "Category", "PCS",
                        "CONSUMABLE", false, null, null, null, "IT", "PROJ-1", "RACK-A", "RACK-A", null,
                        20, 0, 10, 50, false, StockStatus.IN_STOCK, "ACTIVE", 1L, java.time.Instant.now())));

        mvc.perform(put("/api/v1/inventory/stock/ITEM-1/thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentName": "IT",
                                  "projectId": "PROJ-1",
                                  "location": "RACK-A",
                                  "minimumStock": 10,
                                  "maximumStock": 50
                                }
                                """)
                        .principal(new TestingAuthenticationToken("test-user", "credentials")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.minimumStock", is(10)))
                .andExpect(jsonPath("$.maximumStock", is(50)))
                .andExpect(jsonPath("$.departmentName", is("IT")));
    }

    @Test
    void adjustmentEndpointReturnsProjectedBalance() throws Exception {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setBalanceKey("ITEM-1|IT||RACK-A");
        when(adjustment.applyAdjustment("ITEM-1", "IT", null, "RACK-A",
                com.recnaile.inventory.domain.StockType.AVAILABLE, 5, "Physical count", "test-user", "adj-1"))
                .thenReturn(balance);
        when(query.byItem("ITEM-1")).thenReturn(List.of(
                new com.recnaile.inventory.dto.InventoryStockResponse(
                        "ITEM-1|IT||RACK-A", "ITEM-1", "Test item", "Category", "PCS",
                        "CONSUMABLE", false, null, null, null, "IT", null, "RACK-A", "RACK-A", null,
                        5, 0, 0, 0, false, StockStatus.IN_STOCK, "ACTIVE", 1L, java.time.Instant.now())));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/inventory/stock/ITEM-1/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "adj-1")
                        .principal(new TestingAuthenticationToken("test-user", "credentials"))
                        .content("""
                                {
                                  "departmentName": "IT",
                                  "location": "RACK-A",
                                  "stockType": "AVAILABLE",
                                  "delta": 5,
                                  "reason": "Physical count"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is("ITEM-1")))
                .andExpect(jsonPath("$.availableQuantity", is(5)));
    }
    private com.recnaile.inventory.dto.InventoryStockResponse stock(String itemId, int available, int defect) {
        return new com.recnaile.inventory.dto.InventoryStockResponse(
                itemId + "||||", itemId, "Test item", "Category", "PCS", "CONSUMABLE", false,
                null, null, null, "IT", null, "RACK-A", "RACK-A", null, available, defect, 0, 50,
                false, StockStatus.IN_STOCK, "ACTIVE", 0L, java.time.Instant.now());
    }
}
