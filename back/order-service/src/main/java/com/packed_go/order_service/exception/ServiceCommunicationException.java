package com.packed_go.order_service.exception;

public class ServiceCommunicationException extends RuntimeException {
    
    public ServiceCommunicationException(String message) {
        super(message);
    }
    
    public ServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
