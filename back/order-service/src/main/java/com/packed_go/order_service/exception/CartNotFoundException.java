package com.packed_go.order_service.exception;

public class CartNotFoundException extends RuntimeException {
    
    public CartNotFoundException(Long userId) {
        super("No active shopping cart found for user " + userId);
    }
    
    public CartNotFoundException(String message) {
        super(message);
    }
}
