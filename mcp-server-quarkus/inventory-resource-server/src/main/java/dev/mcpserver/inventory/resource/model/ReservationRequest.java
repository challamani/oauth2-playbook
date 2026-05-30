package dev.mcpserver.inventory.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReservationRequest(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("username") String username
) {}
