package com.example.inventory.order;

import com.example.inventory.inventory.Inventory;
import com.example.inventory.inventory.InventoryRepository;
import com.example.inventory.order.dto.CreateOrderRequest;
import com.example.inventory.order.dto.OrderItemResponse;
import com.example.inventory.order.dto.OrderLine;
import com.example.inventory.order.dto.OrderResponse;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request){
        Set<Long> ids = request.items().stream()
                .map(OrderLine::productId)
                .collect(Collectors.toSet());

        if(ids.size() != request.items().size()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate product in order");
        }

        Map<Long, Product> products = productRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        if(products.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        long total = 0;
        for(OrderLine line : request.items()){
            Inventory inventory = inventoryRepository
                    .findById(line.productId())
                    .orElseThrow(()-> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Inventory not found: " + line.productId()
                    ));
            if(inventory.getQuantity() < line.qty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock: "+ line.productId());
            }
            inventory.setQuantity(inventory.getQuantity() - line.qty());
            // ponytail: optimistic check here; Redisson
            // lock in M5 if you need retries/throughput
            inventoryRepository.saveAndFlush(inventory);
            total += products.get(line.productId())
                    .getPriceCents() * line.qty();

        }
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalCents(total);
        orderRepository.save(order);

        List<OrderItem> items = request.items().stream()
                .map(line -> new OrderItem(
                        new OrderItemId(order.getId(),
                                line.productId()),
                        line.qty(),
                        products.get(line.productId()).
                                getPriceCents()))
                .toList();
        orderItemRepository.saveAll(items);

        return new OrderResponse(
                order.getId(), userId, order.
                getOrderStatus().name(), total, order.getCreatedAt(),
                items.stream()
                        .map(i -> new OrderItemResponse(
                                i.getId().getProductId(),
                                i.getQuantity(), i.getPriceCents()))
                        .toList());
    }

}
