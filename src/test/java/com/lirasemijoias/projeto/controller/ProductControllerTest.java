package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService service;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(service);
    }

    @Test
    void findAllPassesSearchAndCategoryFilters() {
        List<Product> products = List.of(new Product());
        when(service.findPublic("brinco", "category-id")).thenReturn(products);

        ResponseEntity<List<Product>> response = controller.findAll("brinco", "category-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(products, response.getBody());
        verify(service).findPublic("brinco", "category-id");
    }

    @Test
    void findAllAcceptsMissingFilters() {
        List<Product> products = List.of(new Product());
        when(service.findPublic(null, null)).thenReturn(products);

        ResponseEntity<List<Product>> response = controller.findAll(null, null);

        assertSame(products, response.getBody());
        verify(service).findPublic(null, null);
    }

    @Test
    void findByIdReturnsActiveProduct() {
        Product product = new Product();
        product.setActive(true);
        when(service.findById("product-id")).thenReturn(product);

        ResponseEntity<Product> response = controller.findById("product-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).findById("product-id");
    }

    @Test
    void findByIdReturnsNotFoundForInactiveProduct() {
        Product product = new Product();
        product.setActive(false);
        when(service.findById("product-id")).thenReturn(product);

        ResponseEntity<Product> response = controller.findById("product-id");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(service).findById("product-id");
    }

    @Test
    void findBySlugReturnsPublicProduct() {
        Product product = new Product();
        when(service.findPublicBySlug("brinco-dourado")).thenReturn(product);

        ResponseEntity<Product> response = controller.findBySlug("brinco-dourado");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).findPublicBySlug("brinco-dourado");
    }
}
