package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.category.CategoryRequest;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private CategoryRepository repository;
    private SlugService slugService;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        slugService = mock(SlugService.class);
        service = new CategoryService(repository, slugService);
    }

    @Test
    void listMethodsDelegateToCorrectQueries() {
        List<Category> publicCategories = List.of(category("1", "A", "a"));
        List<Category> allCategories = List.of(publicCategories.getFirst(), category("2", "B", "b"));
        when(repository.findByActiveTrue()).thenReturn(publicCategories);
        when(repository.findAll()).thenReturn(allCategories);

        assertSame(publicCategories, service.findPublic());
        assertSame(allCategories, service.findAllAdmin());
    }

    @Test
    void findByIdReturnsCategoryOrThrows() {
        Category category = category("1", "Anéis", "aneis");
        when(repository.findById("1")).thenReturn(Optional.of(category));
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertSame(category, service.findById("1"));
        assertThrows(ResourceNotFoundException.class, () -> service.findById("missing"));
    }

    @Test
    void createTrimsNameSetsDefaultsAndAuditFields() {
        CategoryRequest request = new CategoryRequest("  Anéis  ", "Coleção");
        when(slugService.slugify(request.name())).thenReturn("aneis");
        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        Category result = service.create(request);

        assertAll(
                () -> assertEquals("Anéis", result.getName()),
                () -> assertEquals("aneis", result.getSlug()),
                () -> assertEquals("Coleção", result.getDescription()),
                () -> assertTrue(result.isActive()),
                () -> assertFalse(result.getCreatedAt().isBefore(before)),
                () -> assertNotNull(result.getUpdatedAt())
        );
        verify(repository).existsByNameIgnoreCase("  Anéis  ");
        verify(repository).existsBySlug("aneis");
    }

    @Test
    void createRejectsDuplicateNameBeforeGeneratingSlug() {
        CategoryRequest request = new CategoryRequest("Anéis", null);
        when(repository.existsByNameIgnoreCase("Anéis")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(request));

        verifyNoInteractions(slugService);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateSlug() {
        CategoryRequest request = new CategoryRequest("Anéis", null);
        when(slugService.slugify("Anéis")).thenReturn("aneis");
        when(repository.existsBySlug("aneis")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void updateAllowsSameCategorySlugAndChangesFields() {
        Category existing = category("c1", "Old", "old");
        Category sameSlugOwner = category("c1", "Other representation", "novo");
        CategoryRequest request = new CategoryRequest("  Novo  ", "Nova descrição");
        when(repository.findById("c1")).thenReturn(Optional.of(existing));
        when(slugService.slugify(request.name())).thenReturn("novo");
        when(repository.findBySlug("novo")).thenReturn(Optional.of(sameSlugOwner));
        when(repository.save(existing)).thenReturn(existing);

        Category result = service.update("c1", request);

        assertSame(existing, result);
        assertEquals("Novo", existing.getName());
        assertEquals("novo", existing.getSlug());
        assertEquals("Nova descrição", existing.getDescription());
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void updateRejectsSlugOwnedByAnotherCategory() {
        Category existing = category("c1", "Old", "old");
        when(repository.findById("c1")).thenReturn(Optional.of(existing));
        when(slugService.slugify("Novo")).thenReturn("novo");
        when(repository.findBySlug("novo")).thenReturn(Optional.of(category("c2", "Novo", "novo")));

        assertThrows(BusinessException.class,
                () -> service.update("c1", new CategoryRequest("Novo", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void setActiveUpdatesExistingCategory() {
        Category existing = category("c1", "A", "a");
        existing.setActive(true);
        when(repository.findById("c1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Category result = service.setActive("c1", false);

        assertSame(existing, result);
        assertFalse(existing.isActive());
        assertNotNull(existing.getUpdatedAt());
    }

    private static Category category(String id, String name, String slug) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        return category;
    }
}
