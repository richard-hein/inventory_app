package com.example.inventory.common;

import java.util.List;

public record ApiError(int status, String message, List<String> errors) {

    public ApiError(int status, String message) {
        this(status, message, List.of());
    }
}
