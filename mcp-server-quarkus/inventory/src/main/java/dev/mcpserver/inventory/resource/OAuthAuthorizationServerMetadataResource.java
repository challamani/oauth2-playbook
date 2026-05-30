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

@Path("/.well-known/oauth-authorization-server")
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

    @ConfigProperty(name = "inventory.security.tool-scope.events-read")
    String eventsReadScope;

    @ConfigProperty(name = "inventory.security.tool-scope.tickets-read")
    String ticketsReadScope;

    @ConfigProperty(name = "inventory.security.tool-scope.reservations-write")
    String reservationsWriteScope;

    @ConfigProperty(name = "inventory.oauth.agent-client-id")
    String agentClientId;

    @GET
    @PermitAll
    public Map<String, Object> metadata() {
        // This metadata is consumed by MCP-aware AI agents (Claude.ai, Cursor, Cline, etc.).
        // The agent initiates Authorization Code + PKCE flow using these endpoints.
        // It must use agentClientId (public client) - no client_secret needed.
        // Token exchange between this MCP server and Keycloak is an internal concern
        // and is NOT advertised here.
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("issuer", issuer);
        meta.put("authorization_endpoint", authorizationEndpoint);
        meta.put("token_endpoint", tokenEndpoint);
        meta.put("jwks_uri", jwksUri);
        meta.put("response_types_supported", List.of("code"));
        meta.put("grant_types_supported", List.of("authorization_code"));
        meta.put("code_challenge_methods_supported", List.of("S256"));
        // PKCE public client - no client secret
        meta.put("token_endpoint_auth_methods_supported", List.of("none"));
        // Hints to MCP clients which client_id to use for the PKCE flow
        meta.put("registration_endpoint_client_id", agentClientId);
        meta.put("scopes_supported", List.of(eventsReadScope, ticketsReadScope, reservationsWriteScope));
        return meta;
    }
}
