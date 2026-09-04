package com.lirasemijoias.projeto.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.lirasemijoias.projeto.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CloudinaryServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private MultipartFile file;
    private CloudinaryService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        file = mock(MultipartFile.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        service = new CloudinaryService(cloudinary);
    }

    @Test
    void uploadReturnsOnlySecureUrlAndPublicId() throws IOException {
        byte[] bytes = {1, 2, 3};
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(bytes);
        when(uploader.upload(eq(bytes), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://cdn.example/image.png",
                "public_id", "products/image",
                "ignored", "value"));

        Map<String, String> result = service.upload(file);

        assertEquals(Map.of("url", "https://cdn.example/image.png", "publicId", "products/image"), result);
        var options = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(bytes), options.capture());
        assertEquals("lira-semijoias/products", options.getValue().get("folder"));
        assertEquals("image", options.getValue().get("resource_type"));
    }

    @Test
    void uploadRejectsEmptyFileBeforeReadingContent() {
        when(file.isEmpty()).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.upload(file));

        verify(file, never()).getContentType();
        verifyNoInteractions(uploader);
    }

    @Test
    void uploadRejectsNullOrNonImageContentType() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn(null, "application/pdf");

        assertThrows(BusinessException.class, () -> service.upload(file));
        assertThrows(BusinessException.class, () -> service.upload(file));

        verifyNoInteractions(uploader);
    }

    @Test
    void uploadWrapsIoFailureFromFileOrCloudinary() throws IOException {
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenThrow(new IOException("disk failure"));

        assertThrows(BusinessException.class, () -> service.upload(file));

        reset(file);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(new byte[]{4});
        when(uploader.upload(any(), any(Map.class))).thenThrow(new IOException("remote failure"));
        assertThrows(BusinessException.class, () -> service.upload(file));
    }

    @Test
    void deleteDelegatesToUploaderAndWrapsIoFailure() throws IOException {
        service.delete("public-id");
        verify(uploader).destroy(eq("public-id"), any(Map.class));

        doThrow(new IOException("remote failure"))
                .when(uploader).destroy(eq("broken"), any(Map.class));
        assertThrows(BusinessException.class, () -> service.delete("broken"));
    }
}
