package com.recnaile.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    private final String itemId; private final int requested; private final int available;
    public InsufficientStockException(String itemId,int requested,int available){
        super("Insufficient available stock for item " + itemId + ": requested=" + requested + ", available=" + available);
        this.itemId=itemId;this.requested=requested;this.available=available;
    }
    public String getItemId(){return itemId;} public int getRequested(){return requested;} public int getAvailable(){return available;}
}
