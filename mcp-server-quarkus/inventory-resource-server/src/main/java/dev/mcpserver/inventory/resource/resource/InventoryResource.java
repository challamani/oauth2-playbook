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
        // Upcoming Events: June 24, 2026 - December 31, 2026
        // UK Events
        seed("EVT-001", "JManc Unconference",               "Manchester",    "2026-07-10", 200,    0.00, "Free community-led unconference where attendees shape the agenda. Celebrating Java and open source community.");
        seed("EVT-002", "London DevOps Conference",         "London",        "2026-07-15", 400,  249.99, "Premier UK DevOps conference featuring talks on containerization, infrastructure automation, and best practices.");
        seed("EVT-003", "UK Java Developer Summit",         "Birmingham",    "2026-07-25", 300,  199.99, "Dedicated UK Java conference bringing together enterprise Java developers for deep technical sessions.");
        seed("EVT-004", "Manchester Tech Talks",            "Manchester",    "2026-08-12", 250,   79.99, "Series of technical talks on cloud platforms, microservices, and modern software architecture in Manchester.");
        seed("EVT-005", "Edinburgh Software Symposium",     "Edinburgh",     "2026-08-28", 280,  189.99, "Scotland's premier software development conference featuring world-class speakers on innovation and technology.");
        seed("EVT-006", "Bristol Cloud and Kubernetes",     "Bristol",       "2026-09-08", 220,  169.99, "Southwest UK's leading conference on cloud-native technologies, Kubernetes, and container orchestration.");
        
        // International Events
        seed("EVT-007", "O'Reilly Open Source Convention",  "Portland",      "2026-07-20", 600,  499.99, "Premier open source conference bringing together developers, architects, and innovators to share knowledge.");
        seed("EVT-008", "CloudNative SecurityCon",          "San Francisco", "2026-08-05", 400,  399.99, "Dedicated conference on cloud-native security, DevSecOps practices, and containerized application safety.");
        seed("EVT-009", "SpringOne",                        "Las Vegas",     "2026-08-20", 500,  349.99, "The definitive Spring and cloud-native Java conference by VMware Tanzu for modern app developers.");
        seed("EVT-010", "JavaOne",                          "San Francisco", "2026-09-15", 600,  599.99, "Premier Java conference bringing together developers for sessions on the latest Java innovations and trends.");
        seed("EVT-011", "Gartner Application Strategies",   "Orlando",       "2026-09-28", 800,  699.99, "Leading enterprise conference on application modernization, cloud strategy, and digital transformation.");
        seed("EVT-012", "QCon London",                      "London",        "2026-10-05", 350,  449.99, "International software conference focused on emerging trends adopted by early adopters and innovators.");
        seed("EVT-013", "Devoxx Morocco",                   "Casablanca",    "2026-10-14", 350,  149.99, "Africa's growing Java and open source conference connecting developers across the African continent.");
        seed("EVT-014", "QCon San Francisco",               "San Francisco", "2026-10-26", 500,  549.99, "International software conference focused on emerging trends adopted by early adopters and innovators.");
        seed("EVT-015", "Devoxx Belgium",                   "Antwerp",       "2026-11-03", 800,  449.99, "Europe's largest Java and JVM conference with deep-dive talks from industry speakers and experts.");
        seed("EVT-016", "AWS re:Invent",                    "Las Vegas",     "2026-11-16", 1000, 899.99, "AWS's flagship conference showcasing cloud innovation, serverless, AI/ML, and enterprise solutions.");
        seed("EVT-017", "KubeCon NA",                       "Atlanta",       "2026-11-18", 700,  649.99, "North America's biggest cloud-native event bringing together Kubernetes and CNCF project communities.");
        seed("EVT-018", "Jfokus",                           "Stockholm",     "2026-12-02", 400,  299.99, "Nordic Java conference featuring talks on Java platform, cloud technologies, and software development.");
        seed("EVT-019", "Devoxx Greece",                    "Athens",        "2026-12-07", 300,  199.99, "Greek Java conference bringing together developers from across Europe for technical sessions and networking.");
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
