package com.lirasemijoias.projeto.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(
        @Valid @NotNull CustomerRequest customer,
        @Valid @NotNull DeliveryRequest delivery,
        @Valid @NotEmpty List<OrderItemRequest> items,
        @Size(max = 500) String notes
) {}
