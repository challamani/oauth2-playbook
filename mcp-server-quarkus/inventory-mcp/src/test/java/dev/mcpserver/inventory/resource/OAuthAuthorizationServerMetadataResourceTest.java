package dev.mcpserver.inventory.resource;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuthAuthorizationServerMetadataResourceTest {

    @Test
    void metadataReturnsOAuthDescriptorWithoutAuthRequirements() {
        OAuthAuthorizationServerMetadataResource resource = new OAuthAuthorizationServerMetadataResource();
        resource.issuer = "https://issuer";
        resource.authorizationEndpoint = "https://issuer/auth";
        resource.tokenEndpoint = "https://issuer/token";
        resource.jwksUri = "https://issuer/certs";
        resource.eventsReadScope = "mcp:events:read";
        resource.ticketsReadScope = "mcp:tickets:read";
        resource.reservationsWriteScope = "mcp:reservations:write";
        resource.agentClientId = "oauth2-playbook-mcp-agent";

        Map<String, Object> metadata = resource.metadata();

        assertEquals("https://issuer", metadata.get("issuer"));
        assertEquals("https://issuer/auth", metadata.get("authorization_endpoint"));
        assertEquals("https://issuer/token", metadata.get("token_endpoint"));
        assertEquals("https://issuer/certs", metadata.get("jwks_uri"));
        // Agent-facing: authorization_code + PKCE (S256). Token-exchange is internal.
        assertEquals(List.of("code"), metadata.get("response_types_supported"));
        assertEquals(List.of("authorization_code"), metadata.get("grant_types_supported"));
        assertEquals(List.of("S256"), metadata.get("code_challenge_methods_supported"));
        assertEquals(List.of("none"), metadata.get("token_endpoint_auth_methods_supported"));
        assertEquals("oauth2-playbook-mcp-agent", metadata.get("registration_endpoint_client_id"));
        assertEquals(List.of("mcp:events:read", "mcp:tickets:read", "mcp:reservations:write"), metadata.get("scopes_supported"));
    }
}
