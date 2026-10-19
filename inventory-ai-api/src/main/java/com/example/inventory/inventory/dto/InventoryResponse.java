package com.example.inventory.inventory.dto;

import java.time.Instant;

public record InventoryResponse(
        Long productId,
        int quantity,
        Long version,
        Instant updatedAt
) {
}
