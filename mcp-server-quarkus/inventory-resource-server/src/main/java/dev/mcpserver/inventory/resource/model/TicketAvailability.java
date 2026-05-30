package dev.mcpserver.inventory.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TicketAvailability(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("availableTickets") int availableTickets,
        @JsonProperty("pricePerTicket") double pricePerTicket
) {}
