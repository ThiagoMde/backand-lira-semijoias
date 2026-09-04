package com.lirasemijoias.projeto.dto.order;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String name,
        @NotBlank String phone
) {}
