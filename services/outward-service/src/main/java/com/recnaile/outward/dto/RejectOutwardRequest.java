package com.recnaile.outward.dto;

import jakarta.validation.constraints.Size;

public class RejectOutwardRequest {

    @Size(max = 500)
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
