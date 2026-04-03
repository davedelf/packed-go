package com.packed_go.order_service.exception;

public class CartExpiredException extends RuntimeException {
    
    public CartExpiredException(Long cartId) {
        super("Shopping cart with id " + cartId + " has expired");
    }
    
    public CartExpiredException(String message) {
        super(message);
    }
}
