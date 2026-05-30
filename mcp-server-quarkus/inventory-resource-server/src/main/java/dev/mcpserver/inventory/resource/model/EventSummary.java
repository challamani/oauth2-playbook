package dev.mcpserver.inventory.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventSummary(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("name") String name,
        @JsonProperty("city") String city,
        @JsonProperty("date") String date
) {}
