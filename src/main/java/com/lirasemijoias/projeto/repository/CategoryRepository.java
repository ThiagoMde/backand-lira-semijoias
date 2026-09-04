package com.lirasemijoias.projeto.repository;

import com.lirasemijoias.projeto.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    List<Category> findByActiveTrue();
}
