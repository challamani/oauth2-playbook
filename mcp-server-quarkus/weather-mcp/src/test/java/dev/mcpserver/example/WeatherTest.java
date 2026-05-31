package dev.mcpserver.example;

import dev.mcpserver.example.model.Alerts;
import dev.mcpserver.example.model.Forecast;
import dev.mcpserver.example.model.ForecastProperties;
import dev.mcpserver.example.model.Period;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherTest {

    @Test
    void extractForecastUrlReturnsJsonStringWithoutQuotes() {
        var weather = new Weather();
        var expected = "https://api.weather.gov/gridpoints/OKX/40,44/forecast";
        JsonObject points = Json.createObjectBuilder()
                .add("properties", Json.createObjectBuilder().add("forecast", expected))
                .build();

        assertEquals(expected, weather.extractForecastUrl(points));
    }

    @Test
    void extractForecastUrlFailsWhenForecastMissing() {
        var weather = new Weather();
        JsonObject points = Json.createObjectBuilder()
                .add("properties", Json.createObjectBuilder())
                .build();

        var error = assertThrows(IllegalArgumentException.class, () -> weather.extractForecastUrl(points));
        assertTrue(error.getMessage().contains("properties.forecast"));
    }

    @Test
    void getForecastPassesCleanUrlToWeatherClient() {
        var expectedUrl = "https://api.weather.gov/gridpoints/OKX/40,44/forecast";
        var weather = new Weather();
        var client = new StubWeatherClient(expectedUrl);
        weather.weatherClient = client;

        var formattedForecast = weather.getForecast(40.0, -73.0);

        assertEquals(expectedUrl, client.lastForecastUrl);
        assertTrue(formattedForecast.contains("Forecast: Clear skies"));
    }

    static class StubWeatherClient implements WeatherClient {
        private final JsonObject points;
        private final Forecast forecast;
        private String lastForecastUrl;

        StubWeatherClient(String forecastUrl) {
            this.points = Json.createObjectBuilder()
                    .add("properties", Json.createObjectBuilder().add("forecast", forecastUrl))
                    .build();
            this.forecast = new Forecast(new ForecastProperties(List.of(
                    new Period("Tonight", 70, "F", "5 mph", "NE", "Clear skies")
            )));
        }

        @Override
        public Alerts getAlerts(String state) {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public JsonObject getPoints(double latitude, double longitude) {
            return points;
        }

        @Override
        public Forecast getForecast(String url) {
            this.lastForecastUrl = url;
            return forecast;
        }
    }
}

