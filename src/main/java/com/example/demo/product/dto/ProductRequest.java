package com.example.demo.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
        @NotNull(message = "Product ID cannot be empty")
        Long productId,

        @NotNull(message = "Quantity cannot be empty")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity
) {

}
