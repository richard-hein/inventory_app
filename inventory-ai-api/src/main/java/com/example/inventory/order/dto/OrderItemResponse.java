package com.example.inventory.order.dto;

public record OrderItemResponse(
        Long productId,
        int qty,
        long priceCents
) {
}
