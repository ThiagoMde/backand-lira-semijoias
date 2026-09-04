package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.order.CheckoutRequest;
import com.lirasemijoias.projeto.dto.order.CheckoutResponse;
import com.lirasemijoias.projeto.dto.order.CustomerRequest;
import com.lirasemijoias.projeto.dto.order.DeliveryRequest;
import com.lirasemijoias.projeto.dto.order.OrderItemRequest;
import com.lirasemijoias.projeto.model.enums.DeliveryType;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService service;

    @InjectMocks
    private OrderController controller;

    @Test
    void checkoutReturnsCreatedOrderData() {
        CheckoutRequest request = new CheckoutRequest(
                new CustomerRequest("Maria", "11999999999"),
                new DeliveryRequest(DeliveryType.PICKUP, null),
                List.of(new OrderItemRequest("product-id", 2)),
                "Presente");
        CheckoutResponse checkout = new CheckoutResponse(
                "order-id", "PED-001", new BigDecimal("99.90"), OrderStatus.PENDING,
                "Mensagem", "https://wa.me/5511999999999");
        when(service.checkout(request)).thenReturn(checkout);

        ResponseEntity<CheckoutResponse> response = controller.checkout(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(checkout, response.getBody());
        verify(service).checkout(request);
    }
}
