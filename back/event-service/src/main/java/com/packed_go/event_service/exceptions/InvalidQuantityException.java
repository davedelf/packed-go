package com.packed_go.event_service.exceptions;

/**
 * Excepción lanzada cuando se proporciona una cantidad inválida.
 */
public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }

    public InvalidQuantityException(Integer quantity, Integer available) {
        super("Invalid quantity: " + quantity + ". Available: " + available);
    }
}
