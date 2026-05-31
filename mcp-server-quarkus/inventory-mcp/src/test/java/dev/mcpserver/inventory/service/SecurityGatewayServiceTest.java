package dev.mcpserver.inventory.service;

import dev.mcpserver.inventory.model.AuthenticatedUser;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityGatewayServiceTest {

    @Test
    void defaultConstructorPathIsCovered() {
        SecurityGatewayService service = new SecurityGatewayService();
        assertThrows(SecurityException.class, service::requireAuthenticatedUser);
    }

    @Test
    void requireAuthenticatedUserRejectsMissingToken() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getRawToken()).thenReturn(" ");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");

        assertThrows(SecurityException.class, service::requireAuthenticatedUser);
    }

    @Test
    void requireAuthenticatedUserRejectsInvalidIssuer() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("iss")).thenReturn("other-issuer");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");

        assertThrows(SecurityException.class, service::requireAuthenticatedUser);
    }

    @Test
    void requireAuthenticatedUserRejectsInvalidAudience() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("iss")).thenReturn("issuer");
        when(jwt.getClaim("aud")).thenReturn(List.of("different-audience"));
        when(jwt.getClaim("azp")).thenReturn("another-client");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");

        assertThrows(SecurityException.class, service::requireAuthenticatedUser);
    }

    @Test
    void requireAuthenticatedUserAcceptsMatchingAzpWhenAudDoesNotContainClientId() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("iss")).thenReturn("issuer");
        when(jwt.getClaim("aud")).thenReturn(List.of("account"));
        when(jwt.getClaim("azp")).thenReturn("aud");
        when(jwt.getClaim("preferred_name")).thenReturn("Alice");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");

        assertEquals("Alice", service.requireAuthenticatedUser().username());
    }

    @Test
    void requireAuthenticatedUserUsesPreferredName() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("iss")).thenReturn("issuer");
        when(jwt.getClaim("aud")).thenReturn("aud");
        when(jwt.getClaim("preferred_name")).thenReturn("Alice");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");
        AuthenticatedUser user = service.requireAuthenticatedUser();

        assertEquals("agent-token", user.rawToken());
        assertEquals("Alice", user.username());
    }

    @Test
    void requireAuthenticatedUserFallsBackToPreferredUsername() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("iss")).thenReturn("issuer");
        when(jwt.getClaim("aud")).thenReturn("aud");
        when(jwt.getClaim("preferred_name")).thenReturn(" ");
        when(jwt.getClaim("preferred_username")).thenReturn("alice.user");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");

        assertEquals("alice.user", service.requireAuthenticatedUser().username());
    }

    @Test
    void requireAuthenticatedUserFallsBackToPrincipalNameThenSub() {
        JsonWebToken jwtPrincipal = mockBaseJwt();
        when(jwtPrincipal.getClaim("iss")).thenReturn("issuer");
        when(jwtPrincipal.getClaim("aud")).thenReturn("aud");
        when(jwtPrincipal.getClaim("preferred_name")).thenReturn(null);
        when(jwtPrincipal.getClaim("preferred_username")).thenReturn(null);
        when(jwtPrincipal.getName()).thenReturn("principal-name");

        SecurityGatewayService principalService = new SecurityGatewayService(jwtPrincipal, "issuer", "aud");
        assertEquals("principal-name", principalService.requireAuthenticatedUser().username());

        JsonWebToken jwtSub = mockBaseJwt();
        when(jwtSub.getClaim("iss")).thenReturn("issuer");
        when(jwtSub.getClaim("aud")).thenReturn("aud");
        when(jwtSub.getClaim("preferred_name")).thenReturn(null);
        when(jwtSub.getClaim("preferred_username")).thenReturn(null);
        when(jwtSub.getName()).thenReturn(" ");
        when(jwtSub.getClaim("sub")).thenReturn("subject-user");

        SecurityGatewayService subService = new SecurityGatewayService(jwtSub, "issuer", "aud");
        assertEquals("subject-user", subService.requireAuthenticatedUser().username());
    }

    @Test
    void requireAuthenticatedUserRejectsMissingUsernameClaims() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("iss")).thenReturn("issuer");
        when(jwt.getClaim("aud")).thenReturn("aud");
        when(jwt.getClaim("preferred_name")).thenReturn(null);
        when(jwt.getClaim("preferred_username")).thenReturn(null);
        when(jwt.getName()).thenReturn(" ");
        when(jwt.getClaim("sub")).thenReturn(" ");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "issuer", "aud");

        assertThrows(SecurityException.class, service::requireAuthenticatedUser);
    }

    @Test
    void containsAudienceSupportsStringCollectionAndUnsupportedTypes() {
        SecurityGatewayService service = new SecurityGatewayService(mockBaseJwt(), "issuer", "aud");

        assertTrue(service.containsAudience("aud", "aud"));
        assertTrue(service.containsAudience(List.of("aud", "x"), "aud"));
        assertFalse(service.containsAudience(123, "aud"));
    }

    @Test
    void issuerAndAudienceValidationAreSkippedWhenConfigIsBlank() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("preferred_name")).thenReturn("Agent User");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "", "");

        assertEquals("Agent User", service.requireAuthenticatedUser().username());
    }

    @Test
    void requireAuthenticatedUserWithScopeValidatesScopeClaim() {
        JsonWebToken jwt = mockBaseJwt();
        when(jwt.getClaim("preferred_name")).thenReturn("Agent User");
        when(jwt.getClaim("scope")).thenReturn("mcp:events:read mcp:tickets:read");

        SecurityGatewayService service = new SecurityGatewayService(jwt, "", "");

        assertEquals("Agent User", service.requireAuthenticatedUser("mcp:events:read").username());
        assertThrows(SecurityException.class, () -> service.requireAuthenticatedUser("mcp:reservations:write"));
    }

    private JsonWebToken mockBaseJwt() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getRawToken()).thenReturn("agent-token");
        return jwt;
    }
}
