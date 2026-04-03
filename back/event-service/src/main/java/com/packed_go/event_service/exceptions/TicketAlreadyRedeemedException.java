package com.packed_go.event_service.exceptions;

/**
 * Excepción lanzada cuando un ticket ya ha sido canjeado.
 */
public class TicketAlreadyRedeemedException extends RuntimeException {
    public TicketAlreadyRedeemedException(Long ticketId) {
        super("Ticket with id " + ticketId + " has already been redeemed");
    }

    public TicketAlreadyRedeemedException(String message) {
        super(message);
    }
}
