package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.product.*;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService service;

    public AdminProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(service.findAllAdmin());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<Product> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<Product> update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> status(@PathVariable String id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<Product> addImage(@PathVariable String id, @Valid @RequestBody ProductImageRequest request) {
        return ResponseEntity.ok(service.addImage(id, request));
    }

    @DeleteMapping("/{id}/images")
    public ResponseEntity<Product> removeImage(@PathVariable String id, @RequestParam String publicId) {
        return ResponseEntity.ok(service.removeImage(id, publicId));
    }
}
