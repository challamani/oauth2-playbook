package dev.mcpserver.inventory.service;

import dev.mcpserver.inventory.model.AuthenticatedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SecurityGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityGatewayService.class);

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "inventory.security.required-issuer", defaultValue = "")
    String requiredIssuer;

    @ConfigProperty(name = "inventory.security.required-audience", defaultValue = "")
    String requiredAudience;

    SecurityGatewayService() {
    }

    SecurityGatewayService(JsonWebToken jwt, String requiredIssuer, String requiredAudience) {
        this.jwt = jwt;
        this.requiredIssuer = requiredIssuer;
        this.requiredAudience = requiredAudience;
    }

    public AuthenticatedUser requireAuthenticatedUser() {
        if (jwt == null || isBlank(jwt.getRawToken())) {
            throw new NotAuthorizedException("Missing bearer token", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        logger.debug("Inbound agent token: {}", jwt.getRawToken());

        if (!isBlank(requiredIssuer)) {
            String issuer = claimAsString("iss");
            if (!requiredIssuer.equals(issuer)) {
                throw new NotAuthorizedException("Invalid token issuer", Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        if (!isBlank(requiredAudience)) {
            Object audienceClaim = jwt.getClaim("aud");
            String authorizedParty = claimAsString("azp");
            if (!containsAudience(audienceClaim, requiredAudience)
                    && (isBlank(authorizedParty) || !requiredAudience.equals(authorizedParty))) {
                throw new NotAuthorizedException("Token audience does not match", Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }

        String username = firstNonBlank(
                claimAsString("preferred_name"),
                claimAsString("preferred_username"),
                jwt.getName(),
                claimAsString("sub")
        );

        if (isBlank(username)) {
            throw new NotAuthorizedException("Unable to resolve username from JWT claims", Response.status(Response.Status.UNAUTHORIZED).build());
        }

        return new AuthenticatedUser(jwt.getRawToken(), username);
    }

    public AuthenticatedUser requireAuthenticatedUser(String requiredScope) {
        AuthenticatedUser user = requireAuthenticatedUser();
        if (isBlank(requiredScope)) {
            return user;
        }

        String scopeClaim = claimAsString("scope");
        if (!hasScope(scopeClaim, requiredScope)) {
            throw new ForbiddenException("JWT is missing required tool scope: " + requiredScope);
        }

        return user;
    }

    private String claimAsString(String claimName) {
        Object claim = jwt.getClaim(claimName);
        return claim == null ? null : claim.toString();
    }

    boolean containsAudience(Object audClaim, String required) {
        if (audClaim instanceof String aud) {
            return aud.equals(required);
        }

        if (audClaim instanceof Collection<?> audList) {
            return audList.stream().filter(Objects::nonNull).map(Object::toString).anyMatch(required::equals);
        }

        return false;
    }

    boolean hasScope(String scopeClaim, String requiredScope) {
        if (isBlank(scopeClaim) || isBlank(requiredScope)) {
            return false;
        }

        Set<String> scopes = Arrays.stream(scopeClaim.split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        return scopes.contains(requiredScope);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
