package com.recnaile.outward.domain.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_issue_transactions")
public class IssueTransaction {

    @Id
    private String transactionId;

    @Indexed(unique = true)
    private String outwardId;

    private String itemId;
    private int quantity;
    private String recipientUserId;
    private String recipientName;
    private String departmentId;
    private String projectId;
    private String purpose;
    private String location;
    private String issuerId;
    private String approverId;
    private Instant issuedAt;

    @Version
    private Long version;

    public IssueTransaction() {}

    public IssueTransaction(String transactionId, String outwardId, String itemId, int quantity,
                             String recipientUserId, String recipientName, String departmentId,
                             String projectId, String purpose, String location,
                             String issuerId, String approverId, Instant issuedAt) {
        this.transactionId = transactionId;
        this.outwardId = outwardId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.recipientUserId = recipientUserId;
        this.recipientName = recipientName;
        this.departmentId = departmentId;
        this.projectId = projectId;
        this.purpose = purpose;
        this.location = location;
        this.issuerId = issuerId;
        this.approverId = approverId;
        this.issuedAt = issuedAt;
    }

    public String getTransactionId() { return transactionId; }
    public String getOutwardId() { return outwardId; }
    public String getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getRecipientName() { return recipientName; }
    public String getDepartmentId() { return departmentId; }
    public String getProjectId() { return projectId; }
    public String getPurpose() { return purpose; }
    public String getLocation() { return location; }
    public String getIssuerId() { return issuerId; }
    public String getApproverId() { return approverId; }
    public Instant getIssuedAt() { return issuedAt; }
    public Long getVersion() { return version; }
}
