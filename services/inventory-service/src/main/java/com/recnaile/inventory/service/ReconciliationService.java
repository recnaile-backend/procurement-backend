package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.dto.InventoryReconciliationResponse;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {
    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;

    public ReconciliationService(StockBalanceRepository balances, StockMovementRepository movements){
        this.balances=balances; this.movements=movements;
    }

    public InventoryReconciliationResponse reconcile(String itemId) {
        List<StockBalanceDocument> bs=balances.findByItemId(itemId);
        int available=0, defect=0;
        for(StockBalanceDocument b:bs){available+=b.getAvailableQuantity();defect+=b.getDefectQuantity();}
        int movementAvailable=0,movementDefect=0;
        for(StockMovementDocument m:movements.findAll()){
            if(!itemId.equals(m.getItemId())) continue;
            if(m.getStockType().name().equals("AVAILABLE")) movementAvailable+=m.getDelta();
            else movementDefect+=m.getDelta();
        }
        int availableDifference=available-movementAvailable;
        int defectDifference=defect-movementDefect;
        boolean availableMatches=availableDifference==0;
        boolean defectMatches=defectDifference==0;
        return new InventoryReconciliationResponse(itemId,available,movementAvailable,availableDifference,
                defect,movementDefect,defectDifference,availableMatches,defectMatches,
                availableMatches && defectMatches);
    }
}
