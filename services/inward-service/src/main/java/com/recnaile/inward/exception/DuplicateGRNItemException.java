package com.recnaile.inward.exception;

public class DuplicateGRNItemException extends IllegalArgumentException {
    public DuplicateGRNItemException(String itemName) {
        super("GRN request contains duplicate itemName " + itemName
                + "; each item can appear only once per GRN");
    }
}
