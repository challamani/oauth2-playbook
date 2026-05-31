package dev.mcpserver.example;

import dev.mcpserver.example.model.Alerts;
import dev.mcpserver.example.model.Forecast;
import io.quarkus.rest.client.reactive.Url;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

@RegisterRestClient(baseUri = "https://api.weather.gov")
public interface WeatherClient {
    // Get active alerts for a specific state
    @GET
    @Path("/alerts/active/area/{state}")
    Alerts getAlerts(@RestPath String state);

    // Get point metadata for coordinates
    @GET
    @Path("/points/{latitude},{longitude}")
    JsonObject getPoints(@RestPath double latitude, @RestPath double longitude);

    // Get detailed forecast using dynamically provided URL
    @GET
    @Path("/")
    Forecast getForecast(@Url String url);
}
