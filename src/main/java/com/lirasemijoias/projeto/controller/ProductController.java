package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<Product>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId) {
        return ResponseEntity.ok(service.findPublic(search, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable String id) {
        Product product = service.findById(id);
        return product.isActive() ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<Product> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(service.findPublicBySlug(slug));
    }
}
