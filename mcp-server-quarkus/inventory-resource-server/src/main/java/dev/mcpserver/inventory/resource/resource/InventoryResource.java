package dev.mcpserver.inventory.resource.resource;

import dev.mcpserver.inventory.resource.filters.ScopesAllowed;
import dev.mcpserver.inventory.resource.model.EventSummary;
import dev.mcpserver.inventory.resource.model.ReservationRequest;
import dev.mcpserver.inventory.resource.model.ReservationResult;
import dev.mcpserver.inventory.resource.model.TicketAvailability;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Inventory REST API - the downstream resource server called by the MCP server.
 *
 * All endpoints require a valid Bearer JWT (enforced by Quarkus OIDC).
 * Each endpoint additionally requires a specific OAuth2 scope via @ScopesAllowed:
 *
 *   GET  /api/inventory/events               -> api:inventory:events:read
 *   GET  /api/inventory/events/{id}/tickets  -> api:inventory:tickets:read
 *   POST /api/inventory/reservations         -> api:inventory:reservations:write
 */
@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private static final Logger logger = LoggerFactory.getLogger(InventoryResource.class);
    private final JsonWebToken jwt;

    // In-memory stores
    private static final Map<String, EventSummary>     EVENT_STORE       = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger>    TICKET_STOCK      = new ConcurrentHashMap<>();
    private static final Map<String, Double>           TICKET_PRICE      = new ConcurrentHashMap<>();
    private static final Map<String, ReservationResult> RESERVATION_STORE = new ConcurrentHashMap<>();

    static {
        seed("EVT-001", "JavaOne",                          "San Francisco", "2026-09-15", 500, 199.99, "Premier Java conference bringing together developers for sessions on the latest Java innovations.");
        seed("EVT-002", "Devoxx Belgium",                   "Antwerp",       "2026-11-03", 800, 249.99, "Europe's largest Java and JVM conference with deep-dive talks from top industry speakers.");
        seed("EVT-003", "QCon London",                      "London",        "2026-03-24", 300, 299.99, "International software conference focused on emerging trends adopted by early adopters and innovators.");
        seed("EVT-004", "SpringOne",                        "Las Vegas",     "2026-08-20", 400, 179.99, "The definitive Spring and cloud-native Java conference by VMware Tanzu for modern app developers.");
        seed("EVT-005", "KubeCon EU",                       "Amsterdam",     "2026-04-01", 600, 349.99, "The flagship CNCF conference covering Kubernetes, cloud-native infrastructure, and open source ecosystems.");
        seed("EVT-006", "KubeCon NA",                       "Atlanta",       "2026-11-18", 700, 349.99, "North America's biggest cloud-native event bringing together Kubernetes and CNCF project communities.");
        seed("EVT-007", "VoxxedDays Zurich",                "Zurich",        "2026-03-19", 200, 149.99, "A community-driven developer conference with hands-on sessions on Java, microservices, and DevOps.");
        seed("EVT-008", "Devoxx UK",                        "London",        "2026-05-07", 350, 229.99, "UK edition of Devoxx featuring expert talks on Java, architecture, AI, and cloud technologies.");
        seed("EVT-009", "Devoxx France",                    "Paris",         "2026-04-22", 450, 219.99, "France's largest developer conference with sessions in French and English on all things JVM and beyond.");
        seed("EVT-010", "GeeCon",                           "Krakow",        "2026-05-22", 250,  99.99, "Central Europe's beloved Java conference known for high-quality talks and a strong community spirit.");
        seed("EVT-011", "Devoxx Morocco",                   "Casablanca",    "2026-10-14", 300,  79.99, "Africa's growing Java and open source conference connecting developers across the continent.");
        seed("EVT-012", "JNation",                          "Lisbon",        "2026-06-04", 280, 129.99, "A welcoming Portuguese Java conference celebrating the JVM ecosystem with top speakers and workshops.");
        seed("EVT-013", "Manchester Java Community Unconference", "Manchester", "2026-07-10", 150,   0.00, "Free community-led unconference by jmanc.org where attendees shape the agenda on the day. All welcome!");
    }

    private static void seed(String id, String name, String city, String date, int stock, double price, String description) {
        EVENT_STORE.put(id, new EventSummary(id, name, city, date, description));
        TICKET_STOCK.put(id, new AtomicInteger(stock));
        TICKET_PRICE.put(id, price);
    }

    public InventoryResource(JsonWebToken jwt) {
        this.jwt = jwt;
    }

    // GET /api/inventory/events
    @GET
    @Path("/events")
    @ScopesAllowed("api:inventory:events:read")
    public Response listEvents(
            @QueryParam("city")  String city,
            @QueryParam("limit") Integer limit,
            @QueryParam("sort")  String sort) {
        logger.info("listEvents - caller={} city={} limit={}", resolveUsername(), city, limit);
        int cap = (limit == null || limit <= 0) ? 10 : limit;
        List<EventSummary> results = EVENT_STORE.values().stream()
                .filter(e -> city == null || city.isBlank() || e.city().equalsIgnoreCase(city))
                .sorted(Comparator.comparing(EventSummary::date))
                .limit(cap)
                .collect(Collectors.toList());
        return Response.ok(results).build();
    }

    // GET /api/inventory/events/{eventId}/tickets
    @GET
    @Path("/events/{eventId}/tickets")
    @ScopesAllowed("api:inventory:tickets:read")
    public Response getTicketAvailability(@PathParam("eventId") String eventId) {
        logger.info("getTicketAvailability - caller={} eventId={}", resolveUsername(), eventId);
        if (!EVENT_STORE.containsKey(eventId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Event not found: " + eventId).build();
        }
        int available = TICKET_STOCK.get(eventId).get();
        double price  = TICKET_PRICE.get(eventId);
        return Response.ok(new TicketAvailability(eventId, available, price)).build();
    }

    // POST /api/inventory/reservations
    @POST
    @Path("/reservations")
    @ScopesAllowed("api:inventory:reservations:write")
    public Response reserve(ReservationRequest req) {
        logger.info("reserve - caller={} eventId={} qty={}", resolveUsername(), req.eventId(), req.quantity());
        if (req.quantity() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Quantity must be greater than zero").build();
        }
        if (!EVENT_STORE.containsKey(req.eventId())) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Event not found: " + req.eventId()).build();
        }
        AtomicInteger stock = TICKET_STOCK.get(req.eventId());
        int remaining = stock.addAndGet(-req.quantity());
        if (remaining < 0) {
            stock.addAndGet(req.quantity()); // rollback
            return Response.status(Response.Status.CONFLICT)
                    .entity("Not enough tickets for event: " + req.eventId()).build();
        }
        String id = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReservationResult result = new ReservationResult(id, req.eventId(), req.username(), req.quantity(), "CONFIRMED");
        RESERVATION_STORE.put(id, result);
        logger.info("Reservation confirmed - id={} event={} user={} qty={}", id, req.eventId(), req.username(), req.quantity());
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    private String resolveUsername() {
        if (jwt == null) return "anonymous";
        String preferred = jwt.getClaim("preferred_username");
        if (preferred != null && !preferred.isBlank()) return preferred;
        String sub = jwt.getClaim("sub");
        return sub != null ? sub : "unknown";
    }
}
