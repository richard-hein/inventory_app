package com.example.inventory.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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

    @Setter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name="user_roles",
            joinColumns = @JoinColumn(name="user_id"),
            inverseJoinColumns = @JoinColumn(name="role_id")
    )
    private Set<Role> roles = new HashSet<>();


}
