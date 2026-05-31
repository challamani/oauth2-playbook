package dev.mcpserver.inventory.model;

public record ReservationResult(String reservationId, String eventId, String username, int quantity, String status) {
}
