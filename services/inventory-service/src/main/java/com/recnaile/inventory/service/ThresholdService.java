package com.recnaile.inventory.service;

import com.recnaile.inventory.domain.StockBalanceDocument;
import com.recnaile.inventory.domain.StockStatus;
import com.recnaile.inventory.repository.StockBalanceRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThresholdService {
    private final StockBalanceRepository balances;
    public ThresholdService(StockBalanceRepository balances){this.balances=balances;}

    @Transactional
    public StockBalanceDocument update(String itemId,String departmentId,String projectId,String location,double minimum,double maximum){
        if(minimum<0 || maximum<0 || maximum<minimum) throw new IllegalArgumentException("Invalid min/max stock thresholds");
        String key=InventoryEventService.key(itemId,departmentId,projectId,location);
        StockBalanceDocument b=balances.findByBalanceKey(key).orElseGet(StockBalanceDocument::new);
        b.setBalanceKey(key); b.setItemId(itemId); b.setDepartmentId(departmentId); b.setProjectId(projectId); b.setLocation(location);
        b.setMinimumStock(minimum); b.setMaximumStock(maximum);
        b.setLowStock(b.getAvailableQuantity()>0 && b.getAvailableQuantity()<=minimum);
        b.setStatus(b.getAvailableQuantity()<=0?StockStatus.OUT_OF_STOCK:(b.isLowStock()?StockStatus.LOW_STOCK:StockStatus.IN_STOCK));
        b.setUpdatedAt(Instant.now());
        return balances.save(b);
    }
}
