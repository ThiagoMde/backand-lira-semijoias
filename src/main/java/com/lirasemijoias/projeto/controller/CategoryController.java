package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<Category>> findAll() {
        return ResponseEntity.ok(service.findPublic());
    }
}
