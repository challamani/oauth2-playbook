package dev.mcpserver.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TicketAvailability(
        String eventId,
        int availableTickets,
        @JsonProperty("ticketPrice") double ticketPrice
) {
}
