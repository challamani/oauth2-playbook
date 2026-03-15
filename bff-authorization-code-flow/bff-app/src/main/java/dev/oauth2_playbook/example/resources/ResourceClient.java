package dev.oauth2_playbook.example.resources;

import java.util.List;

import dev.oauth2_playbook.example.models.User;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/users")
@RegisterRestClient(configKey = "resource-api")
public interface ResourceClient {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<User> getUsers(@HeaderParam("Authorization") String authHeader);
}
