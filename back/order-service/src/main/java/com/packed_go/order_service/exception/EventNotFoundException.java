package com.packed_go.order_service.exception;

public class EventNotFoundException extends RuntimeException {
    
    public EventNotFoundException(Long eventId) {
        super("Event with id " + eventId + " not found");
    }
    
    public EventNotFoundException(String message) {
        super(message);
    }
    
    public EventNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
