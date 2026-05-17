package dev.mcpserver.example;

import dev.mcpserver.example.model.Alerts;
import dev.mcpserver.example.model.Forecast;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkus.qute.Qute;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.stream.Collectors;

public class Weather {

    @RestClient
    WeatherClient weatherClient;

    @Tool(description = "Get weather alerts for a US state.")
    String getAlerts(@ToolArg(description = "Two-letter US state code (e.g. CA, NY)") String state) {
        return formatAlerts(weatherClient.getAlerts(state));
    }

    String formatAlerts(Alerts alerts) {
        return alerts.features().stream().map(feature -> {
            return Qute.fmt(
                    """
                            Event: {p.event}
                            Area: {p.areaDesc}
                            Severity: {p.severity}
                            Description: {p.description}
                            Instructions: {p.instruction}
                            """,
                    Map.of("p", feature.properties()));
        }).collect(Collectors.joining("\n---\n"));
    }

    String formatForecast(Forecast forecast) {
        return forecast.properties().periods().stream().map(period -> {

            return Qute.fmt(
                    """
                            Temperature: {p.temperature}°{p.temperatureUnit}
                            Wind: {p.windSpeed} {p.windDirection}
                            Forecast: {p.detailedForecast}
                            """,
                    Map.of("p", period));
        }).collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = "Get weather forecast for a location.")
    String getForecast(@ToolArg(description = "Latitude of the location") double latitude,
                       @ToolArg(description = "Longitude of the location") double longitude) {
        var points = weatherClient.getPoints(latitude, longitude);
        var url = extractForecastUrl(points);

        return formatForecast(weatherClient.getForecast(url));
    }

    String extractForecastUrl(JsonObject points) {
        var properties = points.getJsonObject("properties");
        if (properties == null || !properties.containsKey("forecast")) {
            throw new IllegalArgumentException("Missing properties.forecast in weather.gov points response");
        }

        var forecastValue = properties.get("forecast");
        if (forecastValue == null || forecastValue.getValueType() == JsonValue.ValueType.NULL) {
            throw new IllegalArgumentException("Missing properties.forecast in weather.gov points response");
        }
        if (forecastValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new IllegalArgumentException("Expected properties.forecast to be a JSON string");
        }

        return ((JsonString) forecastValue).getString();
    }
}
