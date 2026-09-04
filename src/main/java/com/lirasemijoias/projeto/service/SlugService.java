package com.lirasemijoias.projeto.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class SlugService {
    public String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
