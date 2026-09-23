package com.recnaile.purchaseorder.exception;

public class MasterDataIntegrationException extends RuntimeException {

    private final String code;

    public MasterDataIntegrationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public MasterDataIntegrationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
