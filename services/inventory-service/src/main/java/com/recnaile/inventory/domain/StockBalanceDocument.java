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
    private String departmentId;
    private String projectId;
    private String location;
    private double availableQuantity;
    private double defectQuantity;
    private double minimumStock;
    private double maximumStock;
    private boolean lowStock;
    private StockStatus status;
    @Version private Long version;
    private Instant updatedAt;

    public StockBalanceDocument() {}
    public String getId(){return id;} public String getBalanceKey(){return balanceKey;}
    public void setBalanceKey(String v){balanceKey=v;} public String getItemId(){return itemId;}
    public void setItemId(String v){itemId=v;} public String getDepartmentId(){return departmentId;}
    public void setDepartmentId(String v){departmentId=v;} public String getProjectId(){return projectId;}
    public void setProjectId(String v){projectId=v;} public String getLocation(){return location;}
    public void setLocation(String v){location=v;} public double getAvailableQuantity(){return availableQuantity;}
    public void setAvailableQuantity(double v){availableQuantity=v;} public double getDefectQuantity(){return defectQuantity;}
    public void setDefectQuantity(double v){defectQuantity=v;} public double getMinimumStock(){return minimumStock;}
    public void setMinimumStock(double v){minimumStock=v;} public double getMaximumStock(){return maximumStock;}
    public void setMaximumStock(double v){maximumStock=v;} public boolean isLowStock(){return lowStock;}
    public void setLowStock(boolean v){lowStock=v;} public StockStatus getStatus(){return status;}
    public void setStatus(StockStatus v){status=v;} public Long getVersion(){return version;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
