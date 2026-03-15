package dev.oauth2_playbook.example.resources;


import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.oauth2_playbook.example.filters.ScopesAllowed;
import dev.oauth2_playbook.example.models.User;
import dev.oauth2_playbook.example.models.UserEmail;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);
    private static final Map<String, User> userStore = new ConcurrentHashMap<>();

    public UserResource() {
        // Initialize with a sample user
        User sampleUser = new User("SampleUser", "dummy", "Tester", 
        List.of(new UserEmail("NA","NA",false)), 
        "null");
        sampleUser.setId(UUID.randomUUID().toString());
        userStore.put(sampleUser.getId(), sampleUser);
    }


    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ScopesAllowed({"write:user", "users:admin"})
    public Response createUser(User user) {
        logger.info("Creating user: {}", user.userName);
        
        user.setId(UUID.randomUUID().toString());
        
        userStore.put(user.getId(), user);
        return Response
            .status(Response.Status.CREATED)
            .entity(user).build();
    }

    @GET
    @Path("/user/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ScopesAllowed({"read:user", "users:admin"})
    public Response getUser(@PathParam("id") String id) {

        logger.info("Fetching user with ID: {}", id);
        User user = userStore.get(id);
        if (Objects.nonNull(user)) {
            return Response.ok(user).build();
        }
        else{
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @ScopesAllowed({"users:read", "users:admin"})
    public Response getUsers() {

        logger.info("Fetching all users");
        List<User> users = List.copyOf(userStore.values());
        return Response.ok(users).build();
    }
    
}
    