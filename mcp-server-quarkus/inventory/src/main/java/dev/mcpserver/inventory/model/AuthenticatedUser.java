package dev.mcpserver.inventory.model;

public record AuthenticatedUser(String rawToken, String username) {
}
