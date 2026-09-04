package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.category.CategoryRequest;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repository;
    private final SlugService slugService;

    public CategoryService(CategoryRepository repository, SlugService slugService) {
        this.repository = repository;
        this.slugService = slugService;
    }

    public List<Category> findPublic() {
        return repository.findByActiveTrue();
    }

    public List<Category> findAllAdmin() {
        return repository.findAll();
    }

    public Category findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
    }

    public Category create(CategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Já existe uma categoria com esse nome.");
        }

        String slug = slugService.slugify(request.name());
        if (repository.existsBySlug(slug)) {
            throw new BusinessException("Já existe uma categoria com esse slug.");
        }

        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return repository.save(category);
    }

    public Category update(String id, CategoryRequest request) {
        Category category = findById(id);
        String slug = slugService.slugify(request.name());

        repository.findBySlug(slug)
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new BusinessException("Slug de categoria já utilizado."); });

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setUpdatedAt(LocalDateTime.now());
        return repository.save(category);
    }

    public Category setActive(String id, boolean active) {
        Category category = findById(id);
        category.setActive(active);
        category.setUpdatedAt(LocalDateTime.now());
        return repository.save(category);
    }
}
