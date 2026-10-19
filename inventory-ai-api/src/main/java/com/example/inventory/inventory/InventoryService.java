package com.example.inventory.inventory;

import com.example.inventory.inventory.dto.InventoryResponse;
import com.example.inventory.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public InventoryResponse get(Long productId){
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseThrow(()-> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventory not found: "+ productId
                ));
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse setQuantity(Long productId, int quantity){
        if(!productRepository.existsById(productId)){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found: "+ productId
            );
        }
        Inventory inventory = inventoryRepository.findById(productId)
                .orElseGet(()-> new Inventory(productId));

        inventory.setQuantity(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);

    }
}
