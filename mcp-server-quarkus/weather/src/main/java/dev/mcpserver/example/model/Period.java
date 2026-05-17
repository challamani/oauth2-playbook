package dev.mcpserver.example.model;

public record Period(
        String name,
        int temperature,
        String temperatureUnit,
        String windSpeed,
        String windDirection,
        String detailedForecast) {
}
