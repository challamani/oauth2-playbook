package dev.mcpserver.inventory.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventSummaryJsonTest {

    @Test
    void deserializesNameAsTitleForResourceServerPayload() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "eventId": "EVT-003",
                  "name": "Cloud Summit",
                  "city": "London",
                  "date": "2026-03-24",
                  "description": "A community cloud summit for developers."
                }
                """;

        EventSummary event = mapper.readValue(json, EventSummary.class);

        assertEquals("EVT-003", event.eventId());
        assertEquals("Cloud Summit", event.title());
        assertEquals("London", event.city());
        assertEquals("2026-03-24", event.date());
        assertEquals("A community cloud summit for developers.", event.description());
    }

    @Test
    void toleratesMissingDescriptionField() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "eventId": "EVT-001",
                  "title": "JavaOne",
                  "city": "San Francisco",
                  "date": "2026-09-15"
                }
                """;

        EventSummary event = mapper.readValue(json, EventSummary.class);

        assertEquals("EVT-001", event.eventId());
        assertNull(event.description());
    }
}
