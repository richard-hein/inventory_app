package com.example.inventory.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// CREATE TABLE users (
//    id            BIGSERIAL PRIMARY KEY,
//    email         VARCHAR(255) NOT NULL UNIQUE,
//    password_hash VARCHAR(255) NOT NULL,
//    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
//    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
//);
@Entity
@Table(name="users")
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Setter
    @Column(name="password_hash",nullable = false)
    private String passwordHash;

    @Setter
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled = true;

    @Column(name="created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        if(createdAt == null) createdAt = now;

    }


}
