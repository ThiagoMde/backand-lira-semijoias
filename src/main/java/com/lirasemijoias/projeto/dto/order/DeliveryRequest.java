package com.lirasemijoias.projeto.dto.order;

import com.lirasemijoias.projeto.model.enums.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DeliveryRequest(
        @NotNull DeliveryType type,
        @Valid AddressRequest address
) {}
