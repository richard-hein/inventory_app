package com.example.inventory.order;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
// ID type is the composite key class,
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
    @Query("select i from orderItem i where i.id.orderId= :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}
