package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.order.UpdateOrderStatusRequest;
import com.lirasemijoias.projeto.model.Order;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class AdminOrderController {
    private final OrderService service;

    public AdminOrderController(OrderService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<Order>> findAll(@RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(service.findAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Order> confirm(@PathVariable String id) {
        return ResponseEntity.ok(service.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(@PathVariable String id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable String id,
                                              @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request.status()));
    }
}
