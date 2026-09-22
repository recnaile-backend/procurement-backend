package com.recnaile.indent.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public class IndentItem {

    private String serialNumber;
    private String itemName;
    private Integer quantity;
    private BigDecimal estimatedUnitPrice;
    private BigDecimal estimatedLineAmount;
    private String remarks;
    private String verificationStatus;
    private String verificationMessage;
    private Instant verifiedAt;

    public IndentItem() {
    }

    public IndentItem(
            String serialNumber,
            String itemName,
            Integer quantity,
            BigDecimal estimatedUnitPrice,
            BigDecimal estimatedLineAmount,
            String remarks) {
        this.serialNumber = serialNumber;
        this.itemName = itemName;
        this.quantity = quantity;
        this.estimatedUnitPrice = estimatedUnitPrice;
        this.estimatedLineAmount = estimatedLineAmount;
        this.remarks = remarks;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getEstimatedUnitPrice() {
        return estimatedUnitPrice;
    }

    public void setEstimatedUnitPrice(BigDecimal estimatedUnitPrice) {
        this.estimatedUnitPrice = estimatedUnitPrice;
    }

    public BigDecimal getEstimatedLineAmount() {
        return estimatedLineAmount;
    }

    public void setEstimatedLineAmount(BigDecimal estimatedLineAmount) {
        this.estimatedLineAmount = estimatedLineAmount;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getVerificationMessage() {
        return verificationMessage;
    }

    public void setVerificationMessage(String verificationMessage) {
        this.verificationMessage = verificationMessage;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

}
