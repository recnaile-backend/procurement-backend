package com.recnaile.inventory.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("stock_balances")
public class StockBalanceDocument {
    @Id private String id;
    private String balanceKey;
    private String itemId;
    private String departmentName;
    private String projectId;
    private String location;
    private int availableQuantity;
    private int defectQuantity;
    private int minimumStock;
    private int maximumStock;
    private boolean lowStock;
    private StockStatus status;
    @Version private Long version;
    private Instant updatedAt;

    public StockBalanceDocument() {}
    public String getId(){return id;} public String getBalanceKey(){return balanceKey;}
    public void setBalanceKey(String v){balanceKey=v;} public String getItemId(){return itemId;}
    public void setItemId(String v){itemId=v;} public String getDepartmentName(){return departmentName;}
    public void setDepartmentName(String v){departmentName=v;} public String getProjectId(){return projectId;}
    public void setProjectId(String v){projectId=v;} public String getLocation(){return location;}
    public void setLocation(String v){location=v;} public int getAvailableQuantity(){return availableQuantity;}
    public void setAvailableQuantity(int v){availableQuantity=v;} public int getDefectQuantity(){return defectQuantity;}
    public void setDefectQuantity(int v){defectQuantity=v;} public int getMinimumStock(){return minimumStock;}
    public void setMinimumStock(int v){minimumStock=v;} public int getMaximumStock(){return maximumStock;}
    public void setMaximumStock(int v){maximumStock=v;} public boolean isLowStock(){return lowStock;}
    public void setLowStock(boolean v){lowStock=v;} public StockStatus getStatus(){return status;}
    public void setStatus(StockStatus v){status=v;} public Long getVersion(){return version;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
