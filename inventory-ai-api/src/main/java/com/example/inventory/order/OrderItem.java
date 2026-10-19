package com.example.inventory.order;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    @Column(nullable = false)
    private int quantity;

    @Column(name="price_cents", nullable = false)
    private long priceCents;


}
