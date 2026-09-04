package com.lirasemijoias.projeto.controller;

import com.lirasemijoias.projeto.service.CloudinaryService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/uploads")
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
public class AdminUploadController {
    private final CloudinaryService service;

    public AdminUploadController(CloudinaryService service) { this.service = service; }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(file));
    }

    @DeleteMapping("/images")
    public ResponseEntity<Void> delete(@RequestParam String publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}
