package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.stock.StockRequest;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.StockMovement;
import com.lirasemijoias.projeto.model.User;
import com.lirasemijoias.projeto.repository.UserRepository;
import com.lirasemijoias.projeto.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStockControllerTest {

    @Mock
    private StockService service;

    @Mock
    private UserRepository userRepository;

    private AdminStockController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminStockController(service, userRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("employee@lira.com", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void entryUsesCurrentUserIdAndReturnsProduct() {
        StockRequest request = new StockRequest("product-id", 4, "Reposicao");
        User currentUser = new User();
        currentUser.setId("user-id");
        Product product = new Product();
        when(userRepository.findByEmailIgnoreCase("employee@lira.com")).thenReturn(Optional.of(currentUser));
        when(service.entry("product-id", 4, "Reposicao", "user-id")).thenReturn(product);

        ResponseEntity<Product> response = controller.entry(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).entry("product-id", 4, "Reposicao", "user-id");
    }

    @Test
    void exitUsesNullUserIdWhenAuthenticatedUserIsNotStored() {
        StockRequest request = new StockRequest("product-id", 2, "Avaria");
        Product product = new Product();
        when(userRepository.findByEmailIgnoreCase("employee@lira.com")).thenReturn(Optional.empty());
        when(service.exit("product-id", 2, "Avaria", null)).thenReturn(product);

        ResponseEntity<Product> response = controller.exit(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).exit("product-id", 2, "Avaria", null);
    }

    @Test
    void lowReturnsLowStockProducts() {
        List<Product> products = List.of(new Product());
        when(service.lowStock()).thenReturn(products);

        ResponseEntity<List<Product>> response = controller.low();

        assertSame(products, response.getBody());
        verify(service).lowStock();
    }

    @Test
    void movementsReturnsCompleteHistory() {
        List<StockMovement> movements = List.of(new StockMovement());
        when(service.history()).thenReturn(movements);

        ResponseEntity<List<StockMovement>> response = controller.movements();

        assertSame(movements, response.getBody());
        verify(service).history();
    }

    @Test
    void byProductReturnsProductHistory() {
        List<StockMovement> movements = List.of(new StockMovement());
        when(service.historyByProduct("product-id")).thenReturn(movements);

        ResponseEntity<List<StockMovement>> response = controller.byProduct("product-id");

        assertSame(movements, response.getBody());
        verify(service).historyByProduct("product-id");
    }
}
