package dev.mcpserver.inventory.service;

import dev.mcpserver.inventory.client.InventoryResourceClient;
import dev.mcpserver.inventory.model.AuthenticatedUser;
import dev.mcpserver.inventory.model.EventSummary;
import dev.mcpserver.inventory.model.ListEventsRequest;
import dev.mcpserver.inventory.model.ReservationRequest;
import dev.mcpserver.inventory.model.ReservationResult;
import dev.mcpserver.inventory.model.TicketAvailability;
import dev.mcpserver.inventory.model.TicketAvailabilityRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryProxyServiceTest {

    @Test
    void defaultConstructorPathIsCovered() {
        InventoryProxyService service = new InventoryProxyService();
        assertEquals("Bearer token", service.bearer("token"));
    }

    @Test
    void listTopEventsUsesEventsScopeTokenAndFixedSorting() {
        InventoryResourceClient client = mock(InventoryResourceClient.class);
        TokenExchangeService tokenExchangeService = mock(TokenExchangeService.class);
        when(tokenExchangeService.exchangeForEventsRead("subject")).thenReturn("events-token");
        when(client.listEvents("Bearer events-token", "Berlin", 10, "date,asc"))
                .thenReturn(List.of(new EventSummary("EVT-1", "Cloud Conf", "Berlin", "2026-10-21")));

        InventoryProxyService service = new InventoryProxyService(client, tokenExchangeService);

        List<EventSummary> result = service.listTopEvents(new ListEventsRequest("Berlin"), new AuthenticatedUser("subject", "alice"));

        assertEquals(1, result.size());
        assertEquals("EVT-1", result.get(0).eventId());
    }

    @Test
    void availableTicketsUsesTicketsScopeToken() {
        InventoryResourceClient client = mock(InventoryResourceClient.class);
        TokenExchangeService tokenExchangeService = mock(TokenExchangeService.class);
        when(tokenExchangeService.exchangeForTicketsRead("subject")).thenReturn("tickets-token");
        when(client.getTicketAvailability("Bearer tickets-token", "EVT-9"))
                .thenReturn(new TicketAvailability("EVT-9", 20, 0.0));

        InventoryProxyService service = new InventoryProxyService(client, tokenExchangeService);

        TicketAvailability result = service.availableTickets(new TicketAvailabilityRequest("EVT-9"), new AuthenticatedUser("subject", "alice"));

        assertEquals(20, result.availableTickets());
        assertEquals(0.0, result.ticketPrice());
    }

    @Test
    void reserveMapsUsernameAndUsesReservationScopeToken() {
        InventoryResourceClient client = mock(InventoryResourceClient.class);
        TokenExchangeService tokenExchangeService = mock(TokenExchangeService.class);
        when(tokenExchangeService.exchangeForReservationsWrite("subject")).thenReturn("reservation-token");
        when(client.reserve(org.mockito.ArgumentMatchers.eq("Bearer reservation-token"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ReservationResult("R-1", "EVT-3", "alice", 2, "CONFIRMED"));

        InventoryProxyService service = new InventoryProxyService(client, tokenExchangeService);
        ReservationResult result = service.reserve(new ReservationRequest("EVT-3", 2), new AuthenticatedUser("subject", "alice"));

        assertEquals("R-1", result.reservationId());

        ArgumentCaptor<dev.mcpserver.inventory.model.ReservationResourceRequest> payload =
                ArgumentCaptor.forClass(dev.mcpserver.inventory.model.ReservationResourceRequest.class);
        verify(client).reserve(org.mockito.ArgumentMatchers.eq("Bearer reservation-token"), payload.capture());
        assertEquals("EVT-3", payload.getValue().eventId());
        assertEquals(2, payload.getValue().quantity());
        assertEquals("alice", payload.getValue().username());
    }

    @Test
    void bearerPrefixIsApplied() {
        InventoryProxyService service = new InventoryProxyService(mock(InventoryResourceClient.class), mock(TokenExchangeService.class));
        assertEquals("Bearer abc", service.bearer("abc"));
    }
}
