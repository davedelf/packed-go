package com.packed_go.event_service.exceptions;

/**
 * Excepción lanzada cuando un ticket no está activo.
 */
public class InactiveTicketException extends RuntimeException {
    public InactiveTicketException(Long ticketId) {
        super("Ticket with id " + ticketId + " is not active");
    }

    public InactiveTicketException(String message) {
        super(message);
    }
}
