package dev.mcpserver.inventory.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventSummary(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("title") String title,
        @JsonProperty("city") String city,
        @JsonProperty("date") String date
) {}
