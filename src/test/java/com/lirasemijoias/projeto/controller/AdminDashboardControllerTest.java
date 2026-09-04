package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private DashboardService service;

    @InjectMocks
    private AdminDashboardController controller;

    @Test
    void getReturnsDashboardData() {
        Map<String, Object> dashboard = Map.of("orders", 12L, "revenue", 250.0);
        when(service.get()).thenReturn(dashboard);

        ResponseEntity<Map<String, Object>> response = controller.get();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(dashboard, response.getBody());
        verify(service).get();
    }
}
