package com.example.inventory.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Getter
public class Inventory {

    @Id
    @Column(name = "product_id")
    @Setter
    private Long productId;

    @Column(nullable = false)
    @Setter
    private int quantity;

    @Version
    @Column(nullable = false)
    @Setter
    private Long version;

    @Column(name = "updated_at", nullable = false)
    @Setter
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void updateTimeStamps() {
        this.updatedAt = Instant.now();
    }

}
