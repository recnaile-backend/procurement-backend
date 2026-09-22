package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockMovementDocument;
import com.recnaile.inventory.repository.StockBalanceRepository;
import com.recnaile.inventory.repository.StockMovementRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {
    private final StockBalanceRepository balances; private final StockMovementRepository movements;
    public ReconciliationService(StockBalanceRepository balances,StockMovementRepository movements){this.balances=balances;this.movements=movements;}
    public Map<String,Object> reconcile(String itemId) {
        List<StockBalanceDocument> bs=balances.findByItemId(itemId);
        double available=0, defect=0;
        for(StockBalanceDocument b:bs){available+=b.getAvailableQuantity();defect+=b.getDefectQuantity();}
        double movementAvailable=0,movementDefect=0;
        for(StockMovementDocument m:movements.findAll()){
            if(!itemId.equals(m.getItemId())) continue;
            if(m.getStockType().name().equals("AVAILABLE")) movementAvailable+=m.getDelta(); else movementDefect+=m.getDelta();
        }
        return Map.of("itemId",itemId,"balanceAvailable",available,"movementAvailable",movementAvailable,
            "balanceDefect",defect,"movementDefect",movementDefect,
            "availableMatches",Math.abs(available-movementAvailable)<0.000001,
            "defectMatches",Math.abs(defect-movementDefect)<0.000001);
    }
}
