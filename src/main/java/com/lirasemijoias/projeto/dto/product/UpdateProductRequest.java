package com.lirasemijoias.projeto.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @DecimalMin(value = "0.01") BigDecimal promotionalPrice,
        @NotBlank String categoryId,
        String material,
        String color,
        @PositiveOrZero int minimumStock,
        boolean featured,
        boolean active
) {}
