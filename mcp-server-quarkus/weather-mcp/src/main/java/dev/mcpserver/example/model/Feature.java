package dev.mcpserver.example.model;

public record Feature(
        String id,
        String type,
        Object geometry,
        Properties properties) {
}
