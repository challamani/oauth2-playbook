package dev.mcpserver.inventory.service;

import dev.mcpserver.inventory.client.KeycloakTokenClient;
import dev.mcpserver.inventory.model.TokenExchangeResponse;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenExchangeServiceTest {

    @Test
    void defaultConstructorPathIsCovered() {
        TokenExchangeService service = new TokenExchangeService();
        assertThrows(IllegalArgumentException.class, () -> service.exchange("", "scope"));
    }

    @Test
    void exchangeForEventsReadBuildsExpectedRequestAndReturnsToken() {
        KeycloakTokenClient client = mock(KeycloakTokenClient.class);
        ArgumentCaptor<MultivaluedMap<String, String>> captor = ArgumentCaptor.forClass(MultivaluedMap.class);
        when(client.exchange(captor.capture())).thenReturn(new TokenExchangeResponse("exchanged", "Bearer", 60, "api:inventory:events:read"));

        TokenExchangeService service = new TokenExchangeService(
                client,
                "mcp-client",
                "secret",
                "api:inventory:events:read",
                "api:inventory:tickets:read",
                "api:inventory:reservations:write"
        );

        String token = service.exchangeForEventsRead("subject-token");

        assertEquals("exchanged", token);
        MultivaluedMap<String, String> form = captor.getValue();
        assertEquals("urn:ietf:params:oauth:grant-type:token-exchange", form.getFirst("grant_type"));
        assertEquals("urn:ietf:params:oauth:token-type:access_token", form.getFirst("requested_token_type"));
        assertEquals("subject-token", form.getFirst("subject_token"));
        assertEquals("urn:ietf:params:oauth:token-type:access_token", form.getFirst("subject_token_type"));
        assertEquals("mcp-client", form.getFirst("client_id"));
        assertEquals("secret", form.getFirst("client_secret"));
        assertEquals("api:inventory:events:read", form.getFirst("scope"));
    }

    @Test
    void exchangeForTicketsAndReservationsUseConfiguredScopes() {
        KeycloakTokenClient client = mock(KeycloakTokenClient.class);
        ArgumentCaptor<MultivaluedMap<String, String>> captor = ArgumentCaptor.forClass(MultivaluedMap.class);
        when(client.exchange(captor.capture())).thenReturn(new TokenExchangeResponse("token-1", "Bearer", 60, ""))
                .thenReturn(new TokenExchangeResponse("token-2", "Bearer", 60, ""));

        TokenExchangeService service = new TokenExchangeService(
                client,
                "mcp-client",
                "secret",
                "api:inventory:events:read",
                "api:inventory:tickets:read",
                "api:inventory:reservations:write"
        );

        assertEquals("token-1", service.exchangeForTicketsRead("subject"));
        assertEquals("api:inventory:tickets:read", captor.getAllValues().get(0).getFirst("scope"));

        assertEquals("token-2", service.exchangeForReservationsWrite("subject"));
        assertEquals("api:inventory:reservations:write", captor.getAllValues().get(1).getFirst("scope"));
    }

    @Test
    void exchangeRejectsBlankSubjectToken() {
        TokenExchangeService service = new TokenExchangeService(
                mock(KeycloakTokenClient.class),
                "mcp-client",
                "secret",
                "api:inventory:events:read",
                "api:inventory:tickets:read",
                "api:inventory:reservations:write"
        );

        assertThrows(IllegalArgumentException.class, () -> service.exchangeForEventsRead(" "));
        assertThrows(IllegalArgumentException.class, () -> service.exchangeForEventsRead(null));
    }

    @Test
    void exchangeRejectsMissingAccessTokenInResponse() {
        KeycloakTokenClient client = mock(KeycloakTokenClient.class);
        when(client.exchange(org.mockito.ArgumentMatchers.any())).thenReturn(new TokenExchangeResponse(" ", "Bearer", 60, ""));

        TokenExchangeService service = new TokenExchangeService(
                client,
                "mcp-client",
                "secret",
                "api:inventory:events:read",
                "api:inventory:tickets:read",
                "api:inventory:reservations:write"
        );

        assertThrows(IllegalStateException.class, () -> service.exchangeForEventsRead("subject"));
    }

    @Test
    void exchangeRejectsNullResponse() {
        KeycloakTokenClient client = mock(KeycloakTokenClient.class);
        when(client.exchange(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        TokenExchangeService service = new TokenExchangeService(
                client,
                "mcp-client",
                "secret",
                "api:inventory:events:read",
                "api:inventory:tickets:read",
                "api:inventory:reservations:write"
        );

        assertThrows(IllegalStateException.class, () -> service.exchangeForEventsRead("subject"));
    }

    @Test
    void exchangeRejectsNullAccessTokenField() {
        KeycloakTokenClient client = mock(KeycloakTokenClient.class);
        when(client.exchange(org.mockito.ArgumentMatchers.any())).thenReturn(new TokenExchangeResponse(null, "Bearer", 60, ""));

        TokenExchangeService service = new TokenExchangeService(
                client,
                "mcp-client",
                "secret",
                "mcp:events:read",
                "mcp:tickets:read",
                "mcp:reservations:write"
        );

        assertThrows(IllegalStateException.class, () -> service.exchangeForEventsRead("subject"));
    }
}
