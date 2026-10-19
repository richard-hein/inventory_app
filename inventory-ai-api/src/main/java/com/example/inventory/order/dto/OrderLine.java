package com.example.inventory.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLine(
        @NotNull Long productId,
        @NotNull @Min(1) Integer qty
) {
}
