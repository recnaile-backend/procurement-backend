package com.recnaile.purchaseorder.domain.model;

import jakarta.validation.constraints.Size;

public class InvoiceMetadata {

    @Size(max = 100)
    private String invoiceNumber;

    private String invoiceDate;

    @Size(max = 100)
    private String invoiceStatus;

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
}
