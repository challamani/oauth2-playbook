package dev.mcpserver.inventory.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

@Path("/.well-known")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class OAuthAuthorizationServerMetadataResource {

    @ConfigProperty(name = "inventory.oauth.issuer")
    String issuer;

    @ConfigProperty(name = "inventory.oauth.authorization-endpoint")
    String authorizationEndpoint;

    @ConfigProperty(name = "inventory.oauth.token-endpoint")
    String tokenEndpoint;

    @ConfigProperty(name = "inventory.oauth.jwks-uri")
    String jwksUri;

    @ConfigProperty(name = "inventory.oauth.agent-client-id")
    String agentClientId;

    @ConfigProperty(name = "inventory.security.tool-scope.events-read")
    String eventsReadScope;

    @ConfigProperty(name = "inventory.security.tool-scope.tickets-read")
    String ticketsReadScope;

    @ConfigProperty(name = "inventory.security.tool-scope.reservations-write")
    String reservationsWriteScope;

    @GET
    @Path("/oauth-authorization-server")
    @PermitAll
    public Map<String, Object> metadata() {
        return Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", authorizationEndpoint),
                Map.entry("token_endpoint", tokenEndpoint),
                Map.entry("jwks_uri", jwksUri),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code")),
                Map.entry("code_challenge_methods_supported", List.of("S256")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("none")),
                Map.entry("registration_endpoint_client_id", agentClientId),
                Map.entry("scopes_supported", List.of(eventsReadScope, ticketsReadScope, reservationsWriteScope))
        );
    }
}
