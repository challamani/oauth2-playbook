package dev.mcpserver.inventory.model;

public record ReservationResourceRequest(String eventId, int quantity, String username) {
}
