package dev.mcpserver.inventory.resource.filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JAX-RS request filter that enforces scope-based authorization.
 * Endpoints annotated with {@link ScopesAllowed} will only be accessible
 * when the caller JWT contains at least one of the declared scopes.
 */
@ApplicationScoped
@Provider
public class AccessController implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AccessController.class);

    private final JsonWebToken jwt;
    private final ResourceInfo resourceInfo;

    public AccessController(ResourceInfo resourceInfo, JsonWebToken jwt) {
        this.resourceInfo = resourceInfo;
        this.jwt = jwt;
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        if (resourceInfo == null || resourceInfo.getResourceMethod() == null) {
            return;
        }

        String rawToken = jwt.getRawToken();
        if (rawToken != null && !rawToken.isBlank()) {
            logger.debug("Incoming access token: {}", rawToken);
        } else {
            logger.debug("No access token present in request");
        }

        ScopesAllowed annotation = resourceInfo.getResourceMethod().getAnnotation(ScopesAllowed.class);
        if (Objects.isNull(annotation)) {
            logger.debug("No @ScopesAllowed on method - skipping scope check");
            return;
        }
        String[] required = annotation.value();
        String jwtScopes = jwt.getClaim("scope");
        if (Objects.isNull(jwtScopes) || !hasRequired(jwtScopes, required)) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Forbidden: JWT does not contain a required scope. Required one of: " + List.of(required))
                    .build());
        }
    }

    private boolean hasRequired(String jwtScopes, String[] required) {
        Set<String> tokenScopes = Arrays.stream(jwtScopes.split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        for (String s : required) {
            if (tokenScopes.contains(s)) {
                logger.debug("Scope check passed - matched scope {}", s);
                return true;
            }
        }
        logger.warn("Scope check failed - required one of {}, token has: {}", List.of(required), tokenScopes);
        return false;
    }
}
