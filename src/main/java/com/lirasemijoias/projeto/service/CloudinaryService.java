package com.lirasemijoias.projeto.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lirasemijoias.projeto.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map<String, String> upload(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("Arquivo vazio.");
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BusinessException("Apenas imagens são permitidas.");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "lira-semijoias/products", "resource_type", "image"));
            return Map.of(
                    "url", result.get("secure_url").toString(),
                    "publicId", result.get("public_id").toString()
            );
        } catch (IOException e) {
            throw new BusinessException("Falha ao enviar imagem ao Cloudinary.");
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new BusinessException("Falha ao excluir imagem do Cloudinary.");
        }
    }
}
