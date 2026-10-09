package com.example.inventory.product;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long priceCents,
        Instant createdAt,
        Instant updatedAt) {
}
