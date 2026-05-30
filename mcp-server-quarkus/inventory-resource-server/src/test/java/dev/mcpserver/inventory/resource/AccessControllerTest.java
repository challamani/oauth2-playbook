package dev.mcpserver.inventory.resource;

import dev.mcpserver.inventory.resource.filters.AccessController;
import dev.mcpserver.inventory.resource.filters.ScopesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessControllerTest {

    @Test
    void allowsWhenAnyRequiredScopeIsPresent() throws Exception {
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        JsonWebToken jwt = mock(JsonWebToken.class);
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);

        Method method = DummyResource.class.getDeclaredMethod("events");
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(jwt.getClaim("scope")).thenReturn("api:inventory:events:read profile");

        AccessController filter = new AccessController(resourceInfo, jwt);
        filter.filter(ctx);

        verify(ctx, never()).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
    }

    @Test
    void rejectsWhenRequiredScopeIsMissing() throws NoSuchMethodException, IOException {
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        JsonWebToken jwt = mock(JsonWebToken.class);
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);

        Method method = DummyResource.class.getDeclaredMethod("tickets");
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(jwt.getClaim("scope")).thenReturn("api:inventory:events:read");

        AccessController filter = new AccessController(resourceInfo, jwt);
        filter.filter(ctx);

        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), captor.getValue().getStatus());
    }

    static class DummyResource {

        @ScopesAllowed("api:inventory:events:read")
        void events() {
        }

        @ScopesAllowed("api:inventory:tickets:read")
        void tickets() {
        }
    }
}

