package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUploadControllerTest {

    @Mock
    private CloudinaryService service;

    @Mock
    private MultipartFile file;

    private AdminUploadController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminUploadController(service);
    }

    @Test
    void uploadReturnsCreatedWithCloudinaryData() {
        Map<String, String> result = Map.of(
                "url", "https://img.test/item.jpg",
                "publicId", "products/item");
        when(service.upload(file)).thenReturn(result);

        ResponseEntity<Map<String, String>> response = controller.upload(file);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(result, response.getBody());
        verify(service).upload(file);
    }

    @Test
    void deleteDelegatesAndReturnsNoContent() {
        ResponseEntity<Void> response = controller.delete("products/item");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(service).delete("products/item");
    }
}
