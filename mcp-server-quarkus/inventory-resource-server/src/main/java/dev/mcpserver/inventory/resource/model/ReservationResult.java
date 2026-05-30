package dev.mcpserver.inventory.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReservationResult(
        @JsonProperty("reservationId") String reservationId,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("username") String username,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("status") String status
) {}
