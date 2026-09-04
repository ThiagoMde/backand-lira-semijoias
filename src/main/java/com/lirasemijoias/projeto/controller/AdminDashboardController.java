package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class AdminDashboardController {
    private final DashboardService service;

    public AdminDashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<Map<String, Object>> get() {
        return ResponseEntity.ok(service.get());
    }
}
