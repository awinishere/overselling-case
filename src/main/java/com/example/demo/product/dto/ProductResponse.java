package com.example.demo.product.dto;

public record ProductResponse(
        boolean success,
        String message,
        Integer remainingStock
) {
}
