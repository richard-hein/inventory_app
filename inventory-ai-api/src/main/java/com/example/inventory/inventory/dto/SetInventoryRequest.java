package com.example.inventory.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SetInventoryRequest(
  @NotNull @PositiveOrZero Integer quantity
) {

}
