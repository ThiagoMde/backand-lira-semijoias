package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class CategoryControllerTest {

    @Mock
    private CategoryService service;

    @InjectMocks
    private CategoryController controller;

    @Test
    void findAllReturnsPublicCategories() {
        List<Category> categories = List.of(new Category());
        when(service.findPublic()).thenReturn(categories);

        ResponseEntity<List<Category>> response = controller.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(categories, response.getBody());
        verify(service).findPublic();
    }
}
