package com.lirasemijoias.projeto.dto.order;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String street,
        @NotBlank String number,
        @NotBlank String district,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        String complement
) {}
