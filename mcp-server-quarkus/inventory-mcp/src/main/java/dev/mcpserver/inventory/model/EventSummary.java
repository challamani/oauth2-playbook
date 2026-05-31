package dev.mcpserver.inventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventSummary(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("title") String title,
        @JsonProperty("city") String city,
        @JsonProperty("date") String date,
        @JsonProperty("description") String description
) {
}
