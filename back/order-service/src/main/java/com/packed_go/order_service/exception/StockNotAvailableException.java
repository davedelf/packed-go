package com.packed_go.order_service.exception;

public class StockNotAvailableException extends RuntimeException {
    
    public StockNotAvailableException(Long eventId) {
        super("Event with id " + eventId + " has no available passes");
    }
    
    public StockNotAvailableException(String message) {
        super(message);
    }
}
