package dev.mcpserver.inventory.resource;

import dev.mcpserver.inventory.resource.model.ReservationRequest;
import dev.mcpserver.inventory.resource.resource.InventoryResource;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryResourceTest {

    @Test
    void listEventsFiltersByCityAndRespectsLimit() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getClaim("preferred_username")).thenReturn("alice");

        InventoryResource resource = new InventoryResource(jwt);
        Response response = resource.listEvents("London", 1, "date,asc");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        @SuppressWarnings("unchecked")
        List<Object> events = (List<Object>) response.getEntity();
        assertEquals(1, events.size());
    }

    @Test
    void getTicketAvailabilityReturnsNotFoundForUnknownEvent() {
        InventoryResource resource = new InventoryResource(mock(JsonWebToken.class));

        Response response = resource.getTicketAvailability("EVT-UNKNOWN");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void reserveReturnsCreatedWhenStockIsAvailable() {
        InventoryResource resource = new InventoryResource(mock(JsonWebToken.class));

        Response response = resource.reserve(new ReservationRequest("EVT-001", 1, "alice"));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("CONFIRMED"));
    }
}

