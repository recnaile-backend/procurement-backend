package com.recnaile.inward.exception;

public class DuplicateGRNItemException extends IllegalArgumentException {
    public DuplicateGRNItemException(String itemId) {
        super("GRN request contains duplicate itemId " + itemId
                + "; each item can appear only once per GRN");
    }
}
