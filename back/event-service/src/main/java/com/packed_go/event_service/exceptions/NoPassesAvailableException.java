package com.packed_go.event_service.exceptions;

/**
 * Excepción lanzada cuando no hay passes disponibles para un evento.
 */
public class NoPassesAvailableException extends RuntimeException {
    public NoPassesAvailableException(Long eventId) {
        super("No available passes for event with id: " + eventId);
    }

    public NoPassesAvailableException(String message) {
        super(message);
    }
}
