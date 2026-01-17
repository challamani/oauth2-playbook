package dev.oauth2_playbook.example.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;



@ApplicationScoped
@Provider
public class AccessController implements ContainerRequestFilter {

    private static final Logger Logger = LoggerFactory.getLogger(AccessController.class);
    private final JsonWebToken jwt;
    private final ResourceInfo resourceInfo;

    public AccessController(ResourceInfo resourceInfo, JsonWebToken jwt) {
            this.resourceInfo = resourceInfo;
            this.jwt = jwt;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ScopesAllowed annotation = resourceInfo.getResourceMethod().getAnnotation(ScopesAllowed.class);
        if(Objects.isNull(annotation)){
            Logger.info("No ScopesAllowed annotation present, skipping scope validation");
            return;
        }
        
        String[] definedScopes = annotation.value();
        String jwtScopes = jwt.getClaim("scope");

        if (Objects.isNull(jwtScopes) || !hasRequiredScope(jwtScopes, definedScopes)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Forbidden: JWT does not have required scopes.")
                    .build());
            return;
        }
    }

    private boolean hasRequiredScope(String jwtScopes, String[] definedScopes) {
        List<String> definedScopesList = List.of(definedScopes);
        boolean hasMatch = definedScopesList.stream()
                .anyMatch(jwtScopes::contains);

        if (hasMatch) {
            Logger.debug("Request allowed, AllowedScopes are: {}, there is a matching scope found in accessToken: [{}]", 
            definedScopes, 
            jwtScopes);
        } else {
            Logger.error("Request denied, AllowedScopes are: {}, no matching scopes found in accessToken: [{}]", 
            definedScopes, 
            jwtScopes);
        }
        return hasMatch;
    }
}