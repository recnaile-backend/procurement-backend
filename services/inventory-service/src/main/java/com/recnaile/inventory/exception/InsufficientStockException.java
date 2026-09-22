package com.recnaile.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    private final String itemId; private final double requested; private final double available;
    public InsufficientStockException(String itemId,double requested,double available){
        super("Insufficient available stock for item "+itemId+". Requested="+requested+", available="+available);
        this.itemId=itemId;this.requested=requested;this.available=available;
    }
    public String getItemId(){return itemId;} public double getRequested(){return requested;} public double getAvailable(){return available;}
}
