package dev.mcpserver.inventory.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventSummaryJsonTest {

    @Test
    void deserializesNameAsTitleForResourceServerPayload() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "eventId": "EVT-003",
                  "name": "Cloud Summit",
                  "city": "London",
                  "date": "2026-03-24"
                }
                """;

        EventSummary event = mapper.readValue(json, EventSummary.class);

        assertEquals("EVT-003", event.eventId());
        assertEquals("Cloud Summit", event.title());
        assertEquals("London", event.city());
        assertEquals("2026-03-24", event.date());
    }
}

