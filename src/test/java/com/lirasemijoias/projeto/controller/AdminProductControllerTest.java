package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.product.CreateProductRequest;
import com.lirasemijoias.projeto.dto.product.ProductImageRequest;
import com.lirasemijoias.projeto.dto.product.UpdateProductRequest;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerTest {

    @Mock
    private ProductService service;

    private AdminProductController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminProductController(service);
    }

    @Test
    void findAllReturnsAllAdminProducts() {
        List<Product> products = List.of(new Product(), new Product());
        when(service.findAllAdmin()).thenReturn(products);

        ResponseEntity<List<Product>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(products, response.getBody());
        verify(service).findAllAdmin();
    }

    @Test
    void createReturnsCreatedProduct() {
        CreateProductRequest request = createRequest();
        Product product = new Product();
        when(service.create(request)).thenReturn(product);

        ResponseEntity<Product> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).create(request);
    }

    @Test
    void updateReturnsUpdatedProduct() {
        UpdateProductRequest request = updateRequest();
        Product product = new Product();
        when(service.update("product-id", request)).thenReturn(product);

        ResponseEntity<Product> response = controller.update("product-id", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).update("product-id", request);
    }

    @Test
    void statusReturnsProductWithUpdatedStatus() {
        Product product = new Product();
        when(service.setActive("product-id", false)).thenReturn(product);

        ResponseEntity<Product> response = controller.status("product-id", false);

        assertSame(product, response.getBody());
        verify(service).setActive("product-id", false);
    }

    @Test
    void addImageReturnsUpdatedProduct() {
        ProductImageRequest request = new ProductImageRequest("https://img.test/item.jpg", "item", true);
        Product product = new Product();
        when(service.addImage("product-id", request)).thenReturn(product);

        ResponseEntity<Product> response = controller.addImage("product-id", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(product, response.getBody());
        verify(service).addImage("product-id", request);
    }

    @Test
    void removeImageReturnsUpdatedProduct() {
        Product product = new Product();
        when(service.removeImage("product-id", "public-id")).thenReturn(product);

        ResponseEntity<Product> response = controller.removeImage("product-id", "public-id");

        assertSame(product, response.getBody());
        verify(service).removeImage("product-id", "public-id");
    }

    private CreateProductRequest createRequest() {
        return new CreateProductRequest("Brinco", "BR-1", "Dourado", BigDecimal.TEN,
                null, "category-id", "Metal", "Dourado", 5, 1, true);
    }

    private UpdateProductRequest updateRequest() {
        return new UpdateProductRequest("Brinco", "BR-1", "Atualizado", BigDecimal.TEN,
                null, "category-id", "Metal", "Dourado", 1, true, true);
    }
}
