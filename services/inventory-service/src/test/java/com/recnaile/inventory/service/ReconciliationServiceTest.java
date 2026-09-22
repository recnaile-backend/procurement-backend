package com.recnaile.inventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.recnaile.inventory.domain.*;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ReconciliationServiceTest {

    @Mock StockBalanceRepository balances;
    @Mock StockMovementRepository movements;
    ReconciliationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ReconciliationService(balances, movements);
    }

    @Test
    void reconcileReportsMatchingProjectedAndLedgerQuantities() {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(8);
        balance.setDefectQuantity(2);

        StockMovementDocument received =
                movement("ITEM-1", StockType.AVAILABLE, 10);
        StockMovementDocument issued =
                movement("ITEM-1", StockType.AVAILABLE, -2);
        StockMovementDocument rejected =
                movement("ITEM-1", StockType.DEFECT, 2);

        when(balances.findByItemId("ITEM-1")).thenReturn(List.of(balance));
        when(movements.findAll()).thenReturn(List.of(received, issued, rejected));

        Map<String, Object> result = service.reconcile("ITEM-1");

        assertEquals(8.0, result.get("balanceAvailable"));
        assertEquals(8.0, result.get("movementAvailable"));
        assertEquals(2.0, result.get("balanceDefect"));
        assertEquals(2.0, result.get("movementDefect"));
        assertEquals(true, result.get("availableMatches"));
        assertEquals(true, result.get("defectMatches"));
    }

    @Test
    void reconcileDetectsLedgerMismatch() {
        StockBalanceDocument balance = new StockBalanceDocument();
        balance.setItemId("ITEM-1");
        balance.setAvailableQuantity(8);
        balance.setDefectQuantity(0);

        when(balances.findByItemId("ITEM-1")).thenReturn(List.of(balance));
        when(movements.findAll()).thenReturn(List.of(
                movement("ITEM-1", StockType.AVAILABLE, 10)));

        Map<String, Object> result = service.reconcile("ITEM-1");

        assertEquals(false, result.get("availableMatches"));
    }

    private StockMovementDocument movement(String itemId, StockType type, double delta) {
        return new StockMovementDocument(
                "event-" + Math.random(), "TEST", "SRC-1", itemId,
                null, null, null, type, Math.abs(delta), delta, "TEST",
                Instant.now(), Instant.now(), "corr", "trace", "actor");
    }
}
