package com.example.inventory.order;

import com.example.inventory.TestcontainersConfiguration;
import com.example.inventory.auth.UserRepository;
import com.example.inventory.inventory.Inventory;
import com.example.inventory.inventory.InventoryRepository;
import com.example.inventory.order.dto.CreateOrderRequest;
import com.example.inventory.order.dto.OrderLine;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class OrderCheckoutTest {

    @Autowired OrderService orderService;
    @Autowired ProductRepository products;
    @Autowired InventoryRepository inventories;
    @Autowired UserRepository users;

    @Test
    void concurrentCheckoutNeverOversells() throws Exception {
        Product product = new Product();
        product.setSku("RACE-" + UUID.randomUUID());
        product.setName("Race product");
        product.setPriceCents(100L);
        Long productId = products.save(product).getId();

        Inventory inventory = new Inventory(productId);
        inventory.setQuantity(10);
        inventories.save(inventory);

        Long userId = users.findByEmail("admin@local").orElseThrow().getId();
        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    orderService.create(userId,
                            new CreateOrderRequest(List.of(new OrderLine(productId, 1))));
                    return true;
                } catch (ResponseStatusException | OptimisticLockingFailureException e) {
                    return false;
                }
            }));
        }

        start.countDown();
        int succeeded = 0;
        for (Future<Boolean> result : results) {
            succeeded += result.get() ? 1 : 0;
        }
        pool.shutdown();

        Inventory after = inventories.findById(productId).orElseThrow();
        assertTrue(succeeded <= 10, "sold more than stock");
        assertTrue(after.getQuantity() >= 0, "stock went negative");
        assertEquals(10 - succeeded, after.getQuantity(), "stock drifted from successful orders");
    }
}
