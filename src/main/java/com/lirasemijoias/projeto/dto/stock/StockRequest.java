package com.lirasemijoias.projeto.dto.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StockRequest(
        @NotBlank String productId,
        @Positive int quantity,
        @NotBlank String reason
) {}
