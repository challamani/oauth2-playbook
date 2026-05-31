package dev.mcpserver.inventory.service;

import dev.mcpserver.inventory.client.KeycloakTokenClient;
import dev.mcpserver.inventory.model.TokenExchangeResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class TokenExchangeService {

    private static final String TOKEN_EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String ACCESS_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";

    @Inject
    @RestClient
    KeycloakTokenClient keycloakTokenClient;

    @ConfigProperty(name = "inventory.token-exchange.client-id")
    String clientId;

    @ConfigProperty(name = "inventory.token-exchange.client-secret")
    String clientSecret;

    @ConfigProperty(name = "inventory.token-exchange.scope.events-read")
    String eventsReadScope;

    @ConfigProperty(name = "inventory.token-exchange.scope.tickets-read")
    String ticketsReadScope;

    @ConfigProperty(name = "inventory.token-exchange.scope.reservations-write")
    String reservationsWriteScope;

    TokenExchangeService() {
    }

    TokenExchangeService(KeycloakTokenClient keycloakTokenClient,
                         String clientId,
                         String clientSecret,
                         String eventsReadScope,
                         String ticketsReadScope,
                         String reservationsWriteScope) {
        this.keycloakTokenClient = keycloakTokenClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.eventsReadScope = eventsReadScope;
        this.ticketsReadScope = ticketsReadScope;
        this.reservationsWriteScope = reservationsWriteScope;
    }

    public String exchangeForEventsRead(String subjectToken) {
        return exchange(subjectToken, eventsReadScope);
    }

    public String exchangeForTicketsRead(String subjectToken) {
        return exchange(subjectToken, ticketsReadScope);
    }

    public String exchangeForReservationsWrite(String subjectToken) {
        return exchange(subjectToken, reservationsWriteScope);
    }

    String exchange(String subjectToken, String scope) {
        if (subjectToken == null || subjectToken.isBlank()) {
            throw new IllegalArgumentException("Subject token is required for token exchange");
        }

        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.add("grant_type", TOKEN_EXCHANGE_GRANT);
        form.add("requested_token_type", ACCESS_TOKEN_TYPE);
        form.add("subject_token", subjectToken);
        form.add("subject_token_type", ACCESS_TOKEN_TYPE);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);

        TokenExchangeResponse response = keycloakTokenClient.exchange(form);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Token exchange did not return an access token");
        }

        return response.accessToken();
    }
}
