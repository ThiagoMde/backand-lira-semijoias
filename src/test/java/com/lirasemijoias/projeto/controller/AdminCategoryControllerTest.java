package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.dto.category.CategoryRequest;
import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryControllerTest {

    @Mock
    private CategoryService service;

    private AdminCategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminCategoryController(service);
    }

    @Test
    void findAllReturnsAllAdminCategories() {
        List<Category> categories = List.of(new Category(), new Category());
        when(service.findAllAdmin()).thenReturn(categories);

        ResponseEntity<List<Category>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(categories, response.getBody());
        verify(service).findAllAdmin();
    }

    @Test
    void createReturnsCreatedCategory() {
        CategoryRequest request = new CategoryRequest("Brincos", "Categoria");
        Category category = new Category();
        when(service.create(request)).thenReturn(category);

        ResponseEntity<Category> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(category, response.getBody());
        verify(service).create(request);
    }

    @Test
    void updateReturnsUpdatedCategory() {
        CategoryRequest request = new CategoryRequest("Aneis", "Categoria atualizada");
        Category category = new Category();
        when(service.update("category-id", request)).thenReturn(category);

        ResponseEntity<Category> response = controller.update("category-id", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(category, response.getBody());
        verify(service).update("category-id", request);
    }

    @Test
    void statusReturnsCategoryWithUpdatedStatus() {
        Category category = new Category();
        when(service.setActive("category-id", false)).thenReturn(category);

        ResponseEntity<Category> response = controller.status("category-id", false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(category, response.getBody());
        verify(service).setActive("category-id", false);
    }
}
