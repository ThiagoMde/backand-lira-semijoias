package com.lirasemijoias.projeto.dto.order;

import com.lirasemijoias.projeto.model.enums.OrderStatus;

import java.math.BigDecimal;

public record CheckoutResponse(
        String orderId,
        String orderNumber,
        BigDecimal total,
        OrderStatus status,
        String whatsappMessage,
        String whatsappUrl
) {}
