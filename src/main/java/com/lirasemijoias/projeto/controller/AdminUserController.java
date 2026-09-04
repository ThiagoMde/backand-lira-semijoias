package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService service;

    public AdminUserController(UserService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<User>> findAll() {
        List<User> users = service.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest request) {
        User user = service.create(request);
        user.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<User> status(@PathVariable String id, @RequestParam boolean active) {
        User user = service.setActive(id, active);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}
