package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.stock.StockRequest;
import com.lirasemijoias.projeto.model.*;
import com.lirasemijoias.projeto.repository.UserRepository;
import com.lirasemijoias.projeto.security.SecurityUtils;
import com.lirasemijoias.projeto.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stock")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class AdminStockController {
    private final StockService service;
    private final UserRepository userRepository;

    public AdminStockController(StockService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping("/entry")
    public ResponseEntity<Product> entry(@Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(service.entry(request.productId(), request.quantity(), request.reason(), currentUserId()));
    }

    @PostMapping("/exit")
    public ResponseEntity<Product> exit(@Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(service.exit(request.productId(), request.quantity(), request.reason(), currentUserId()));
    }

    @GetMapping("/low")
    public ResponseEntity<List<Product>> low() {
        return ResponseEntity.ok(service.lowStock());
    }

    @GetMapping("/movements")
    public ResponseEntity<List<StockMovement>> movements() {
        return ResponseEntity.ok(service.history());
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<List<StockMovement>> byProduct(@PathVariable String productId) {
        return ResponseEntity.ok(service.historyByProduct(productId));
    }

    private String currentUserId() {
        return userRepository.findByEmailIgnoreCase(SecurityUtils.currentUsername())
                .map(User::getId).orElse(null);
    }
}
