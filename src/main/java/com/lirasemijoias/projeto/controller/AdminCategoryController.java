package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.category.CategoryRequest;
import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {
    private final CategoryService service;

    public AdminCategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<Category>> findAll() {
        return ResponseEntity.ok(service.findAllAdmin());
    }

    @PostMapping
    public ResponseEntity<Category> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable String id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Category> status(@PathVariable String id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }
}
