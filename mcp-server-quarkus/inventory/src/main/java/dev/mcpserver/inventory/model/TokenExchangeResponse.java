package dev.mcpserver.inventory.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenExchangeResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("scope") String scope
) {
}
