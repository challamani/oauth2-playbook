package dev.mcpserver.inventory;

import dev.mcpserver.inventory.model.AuthenticatedUser;
import dev.mcpserver.inventory.model.EventSummary;
import dev.mcpserver.inventory.model.ListEventsRequest;
import dev.mcpserver.inventory.model.ReservationRequest;
import dev.mcpserver.inventory.model.ReservationResult;
import dev.mcpserver.inventory.model.TicketAvailability;
import dev.mcpserver.inventory.model.TicketAvailabilityRequest;
import dev.mcpserver.inventory.service.InventoryProxyService;
import dev.mcpserver.inventory.service.OpenApiContractValidator;
import dev.mcpserver.inventory.service.SecurityGatewayService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryToolsTest {

    @Test
    void defaultConstructorPathIsCovered() {
        InventoryTools tools = new InventoryTools();
        assertThrows(NullPointerException.class, () -> tools.listTopEventsByCity("Rome"));
    }

    @Test
    void listTopEventsByCityValidatesAndDelegates() {
        SecurityGatewayService security = mock(SecurityGatewayService.class);
        OpenApiContractValidator validator = mock(OpenApiContractValidator.class);
        InventoryProxyService proxy = mock(InventoryProxyService.class);

        AuthenticatedUser user = new AuthenticatedUser("jwt", "alice");
        when(security.requireAuthenticatedUser(anyString())).thenReturn(user);
        when(proxy.listTopEvents(new ListEventsRequest("London"), user))
                .thenReturn(List.of(new EventSummary("EVT-1", "JavaOne", "London", "2026-01-01", null)));

        InventoryTools tools = new InventoryTools(security, validator, proxy);
        List<EventSummary> events = tools.listTopEventsByCity("London");

        assertEquals(1, events.size());
        verify(validator).validate("ListEventsRequest", new ListEventsRequest("London"));
    }

    @Test
    void availableTicketsAndPriceValidatesAndDelegates() {
        SecurityGatewayService security = mock(SecurityGatewayService.class);
        OpenApiContractValidator validator = mock(OpenApiContractValidator.class);
        InventoryProxyService proxy = mock(InventoryProxyService.class);

        AuthenticatedUser user = new AuthenticatedUser("jwt", "alice");
        when(security.requireAuthenticatedUser(anyString())).thenReturn(user);
        when(proxy.availableTickets(new TicketAvailabilityRequest("EVT-2"), user))
                .thenReturn(new TicketAvailability("EVT-2", 12, 49.99));

        InventoryTools tools = new InventoryTools(security, validator, proxy);
        TicketAvailability response = tools.availableTicketsAndPrice("EVT-2");

        assertEquals("EVT-2", response.eventId());
        verify(validator).validate("TicketAvailabilityRequest", new TicketAvailabilityRequest("EVT-2"));
    }

    @Test
    void reserveEventValidatesAndDelegates() {
        SecurityGatewayService security = mock(SecurityGatewayService.class);
        OpenApiContractValidator validator = mock(OpenApiContractValidator.class);
        InventoryProxyService proxy = mock(InventoryProxyService.class);

        AuthenticatedUser user = new AuthenticatedUser("jwt", "alice");
        when(security.requireAuthenticatedUser(anyString())).thenReturn(user);
        when(proxy.reserve(new ReservationRequest("EVT-3", 2), user))
                .thenReturn(new ReservationResult("R-1", "EVT-3", "alice", 2, "CONFIRMED"));

        InventoryTools tools = new InventoryTools(security, validator, proxy);
        ReservationResult reservation = tools.reserveEvent("EVT-3", 2);

        assertEquals("R-1", reservation.reservationId());
        verify(validator).validate("ReservationRequest", new ReservationRequest("EVT-3", 2));
    }

    @Test
    void listTopEventsByCityStopsWhenValidationFails() {
        SecurityGatewayService security = mock(SecurityGatewayService.class);
        OpenApiContractValidator validator = mock(OpenApiContractValidator.class);
        InventoryProxyService proxy = mock(InventoryProxyService.class);

        AuthenticatedUser user = new AuthenticatedUser("jwt", "alice");
        when(security.requireAuthenticatedUser(anyString())).thenReturn(user);
        doThrow(new IllegalArgumentException("invalid")).when(validator)
                .validate("ListEventsRequest", new ListEventsRequest(""));

        InventoryTools tools = new InventoryTools(security, validator, proxy);

        assertThrows(IllegalArgumentException.class, () -> tools.listTopEventsByCity(""));
    }
}
