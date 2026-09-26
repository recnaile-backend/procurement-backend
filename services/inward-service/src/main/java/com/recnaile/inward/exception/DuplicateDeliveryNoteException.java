package com.recnaile.inward.exception;

public class DuplicateDeliveryNoteException extends RuntimeException {
    public DuplicateDeliveryNoteException(String poId, String deliveryNoteNumber) {
        super("Goods Receipt already exists for Purchase Order " + poId
                + " with delivery note " + deliveryNoteNumber);
    }
}
