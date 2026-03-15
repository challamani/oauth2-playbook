package dev.oauth2_playbook.example.resources;


import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.oauth2_playbook.example.models.User;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api")
@Authenticated
public class UserProxyResource {

    private static final Logger logger = LoggerFactory.getLogger(UserProxyResource.class);

    @Inject
    @RestClient
    ResourceClient resourceClient;

    @Inject
    AccessTokenCredential accessTokenCredential;

    @Inject
    SecurityIdentity identity;


    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {

        logger.info("Fetching all users");
        String accessToken = accessTokenCredential.getToken();
        logger.info("token: {}", accessToken);
        List<User> users = resourceClient.getUsers("Bearer " + accessToken);
        return Response.ok(users).build();
    }

    @GET
    @Authenticated
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getMe() {
        // Return username or claims from the session cookie
        logger.info("Fetching current user info");
        return Map.of("username", identity.getPrincipal().getName());
    }
    
}
    