package dev.mcpserver.inventory.service;

import dev.mcpserver.inventory.client.InventoryResourceClient;
import dev.mcpserver.inventory.model.AuthenticatedUser;
import dev.mcpserver.inventory.model.EventSummary;
import dev.mcpserver.inventory.model.ListEventsRequest;
import dev.mcpserver.inventory.model.ReservationRequest;
import dev.mcpserver.inventory.model.ReservationResourceRequest;
import dev.mcpserver.inventory.model.ReservationResult;
import dev.mcpserver.inventory.model.TicketAvailability;
import dev.mcpserver.inventory.model.TicketAvailabilityRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class InventoryProxyService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryProxyService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final String SORT_DATE_ASC = "date,asc";

    @Inject
    @RestClient
    InventoryResourceClient inventoryResourceClient;

    @Inject
    TokenExchangeService tokenExchangeService;

    InventoryProxyService() {
    }

    InventoryProxyService(InventoryResourceClient inventoryResourceClient, TokenExchangeService tokenExchangeService) {
        this.inventoryResourceClient = inventoryResourceClient;
        this.tokenExchangeService = tokenExchangeService;
    }

    public List<EventSummary> listTopEvents(ListEventsRequest request, AuthenticatedUser user) {
        String token = tokenExchangeService.exchangeForEventsRead(user.rawToken());
        logger.debug("listTopEvents - sending exchanged token to resource server: {}", token);
        return inventoryResourceClient.listEvents(bearer(token), request.city(), DEFAULT_LIMIT, SORT_DATE_ASC);
    }

    public TicketAvailability availableTickets(TicketAvailabilityRequest request, AuthenticatedUser user) {
        String token = tokenExchangeService.exchangeForTicketsRead(user.rawToken());
        logger.debug("availableTickets - sending exchanged token to resource server: {}", token);
        return inventoryResourceClient.getTicketAvailability(bearer(token), request.eventId());
    }

    public ReservationResult reserve(ReservationRequest request, AuthenticatedUser user) {
        String token = tokenExchangeService.exchangeForReservationsWrite(user.rawToken());
        logger.debug("reserve - sending exchanged token to resource server: {}", token);
        ReservationResourceRequest payload = new ReservationResourceRequest(request.eventId(), request.quantity(), user.username());
        return inventoryResourceClient.reserve(bearer(token), payload);
    }

    String bearer(String token) {
        return "Bearer " + token;
    }
}
