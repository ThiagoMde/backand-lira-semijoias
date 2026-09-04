package com.lirasemijoias.projeto.dto.product;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @NotBlank String url,
        @NotBlank String publicId,
        boolean main
) {}
