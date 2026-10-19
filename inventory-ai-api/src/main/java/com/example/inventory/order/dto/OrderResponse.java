package com.example.inventory.order.dto;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String status,
        long totalCents,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
