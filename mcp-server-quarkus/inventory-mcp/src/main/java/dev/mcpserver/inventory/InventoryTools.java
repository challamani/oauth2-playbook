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
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@ApplicationScoped
public class InventoryTools {

    private static final String DEFAULT_EVENTS_SCOPE = "mcp:events:read";
    private static final String DEFAULT_TICKETS_SCOPE = "mcp:tickets:read";
    private static final String DEFAULT_RESERVATIONS_SCOPE = "mcp:reservations:write";

    @Inject
    SecurityGatewayService securityGatewayService;

    @Inject
    OpenApiContractValidator openApiContractValidator;

    @Inject
    InventoryProxyService inventoryProxyService;

    @ConfigProperty(name = "inventory.security.tool-scope.events-read", defaultValue = DEFAULT_EVENTS_SCOPE)
    String eventsReadToolScope;

    @ConfigProperty(name = "inventory.security.tool-scope.tickets-read", defaultValue = DEFAULT_TICKETS_SCOPE)
    String ticketsReadToolScope;

    @ConfigProperty(name = "inventory.security.tool-scope.reservations-write", defaultValue = DEFAULT_RESERVATIONS_SCOPE)
    String reservationsWriteToolScope;

    InventoryTools() {
    }

    InventoryTools(SecurityGatewayService securityGatewayService,
                   OpenApiContractValidator openApiContractValidator,
                   InventoryProxyService inventoryProxyService) {
        this.securityGatewayService = securityGatewayService;
        this.openApiContractValidator = openApiContractValidator;
        this.inventoryProxyService = inventoryProxyService;
        this.eventsReadToolScope = DEFAULT_EVENTS_SCOPE;
        this.ticketsReadToolScope = DEFAULT_TICKETS_SCOPE;
        this.reservationsWriteToolScope = DEFAULT_RESERVATIONS_SCOPE;
    }

    @Tool(description = "List top 10 conference events ordered by date ascending for a city")
    public List<EventSummary> listTopEventsByCity(
            @ToolArg(description = "City name") String city
    ) {
        AuthenticatedUser user = securityGatewayService.requireAuthenticatedUser(eventsReadToolScope);
        ListEventsRequest request = new ListEventsRequest(city);
        openApiContractValidator.validate("ListEventsRequest", request);
        return inventoryProxyService.listTopEvents(request, user);
    }

    @Tool(description = "Get available tickets and ticket price for an event")
    public TicketAvailability availableTicketsAndPrice(
            @ToolArg(description = "Event identifier") String eventId
    ) {
        AuthenticatedUser user = securityGatewayService.requireAuthenticatedUser(ticketsReadToolScope);
        TicketAvailabilityRequest request = new TicketAvailabilityRequest(eventId);
        openApiContractValidator.validate("TicketAvailabilityRequest", request);
        return inventoryProxyService.availableTickets(request, user);
    }

    @Tool(description = "Reserve tickets for one event at a time")
    public ReservationResult reserveEvent(
            @ToolArg(description = "Event identifier") String eventId,
            @ToolArg(description = "Number of tickets to reserve") int quantity
    ) {
        AuthenticatedUser user = securityGatewayService.requireAuthenticatedUser(reservationsWriteToolScope);
        ReservationRequest request = new ReservationRequest(eventId, quantity);
        openApiContractValidator.validate("ReservationRequest", request);
        return inventoryProxyService.reserve(request, user);
    }
}
