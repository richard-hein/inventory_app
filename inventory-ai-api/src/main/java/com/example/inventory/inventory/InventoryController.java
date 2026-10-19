package com.example.inventory.inventory;

import com.example.inventory.inventory.dto.InventoryResponse;
import com.example.inventory.inventory.dto.SetInventoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId){
        return inventoryService.get(productId);
    }

    @PutMapping("/${productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse set(
            @PathVariable Long productId,
            @Valid @RequestBody SetInventoryRequest request
            ){
        return inventoryService.setQuantity(productId, request.quantity());
    }



}
