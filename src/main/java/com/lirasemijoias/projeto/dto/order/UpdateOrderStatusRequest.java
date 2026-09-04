package com.lirasemijoias.projeto.dto.order;

import com.lirasemijoias.projeto.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
