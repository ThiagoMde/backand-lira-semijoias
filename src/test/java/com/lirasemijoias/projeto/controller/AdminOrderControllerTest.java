package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.order.UpdateOrderStatusRequest;
import com.lirasemijoias.projeto.model.Order;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService service;

    private AdminOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminOrderController(service);
    }

    @Test
    void findAllPassesStatusFilterToService() {
        List<Order> orders = List.of(new Order());
        when(service.findAll(OrderStatus.PENDING)).thenReturn(orders);

        ResponseEntity<List<Order>> response = controller.findAll(OrderStatus.PENDING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(orders, response.getBody());
        verify(service).findAll(OrderStatus.PENDING);
    }

    @Test
    void findAllAcceptsMissingStatusFilter() {
        List<Order> orders = List.of(new Order());
        when(service.findAll(null)).thenReturn(orders);

        ResponseEntity<List<Order>> response = controller.findAll(null);

        assertSame(orders, response.getBody());
        verify(service).findAll(null);
    }

    @Test
    void findByIdReturnsOrder() {
        Order order = new Order();
        when(service.findById("order-id")).thenReturn(order);

        ResponseEntity<Order> response = controller.findById("order-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(order, response.getBody());
        verify(service).findById("order-id");
    }

    @Test
    void confirmReturnsConfirmedOrder() {
        Order order = new Order();
        when(service.confirm("order-id")).thenReturn(order);

        ResponseEntity<Order> response = controller.confirm("order-id");

        assertSame(order, response.getBody());
        verify(service).confirm("order-id");
    }

    @Test
    void cancelReturnsCancelledOrder() {
        Order order = new Order();
        when(service.cancel("order-id")).thenReturn(order);

        ResponseEntity<Order> response = controller.cancel("order-id");

        assertSame(order, response.getBody());
        verify(service).cancel("order-id");
    }

    @Test
    void updateStatusPassesStatusFromRequest() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.DELIVERED);
        Order order = new Order();
        when(service.updateStatus("order-id", OrderStatus.DELIVERED)).thenReturn(order);

        ResponseEntity<Order> response = controller.updateStatus("order-id", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(order, response.getBody());
        verify(service).updateStatus("order-id", OrderStatus.DELIVERED);
    }
}
