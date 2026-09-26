package com.recnaile.inward.exception;

public class GRNNotFoundException extends RuntimeException {
    public GRNNotFoundException(String id) {
        super("Goods receipt " + id + " was not found");
    }
}
