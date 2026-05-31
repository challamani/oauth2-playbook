package dev.mcpserver.inventory.client;

import dev.mcpserver.inventory.model.EventSummary;
import dev.mcpserver.inventory.model.ReservationResourceRequest;
import dev.mcpserver.inventory.model.ReservationResult;
import dev.mcpserver.inventory.model.TicketAvailability;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "inventory-api")
@Path("/api/inventory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface InventoryResourceClient {

    @GET
    @Path("/events")
    List<EventSummary> listEvents(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("city") String city,
            @QueryParam("limit") int limit,
            @QueryParam("sort") String sort
    );

    @GET
    @Path("/events/{eventId}/tickets")
    TicketAvailability getTicketAvailability(
            @HeaderParam("Authorization") String authorization,
            @PathParam("eventId") String eventId
    );

    @POST
    @Path("/reservations")
    ReservationResult reserve(
            @HeaderParam("Authorization") String authorization,
            ReservationResourceRequest request
    );
}
