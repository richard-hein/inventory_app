package com.example.inventory.inventory;

import com.example.inventory.inventory.dto.InventoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    InventoryResponse toResponse(Inventory inventory);
}
