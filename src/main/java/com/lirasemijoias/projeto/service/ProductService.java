package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.product.CreateProductRequest;
import com.lirasemijoias.projeto.dto.product.ProductImageRequest;
import com.lirasemijoias.projeto.dto.product.UpdateProductRequest;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.ProductImage;
import com.lirasemijoias.projeto.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final CategoryService categoryService;
    private final SlugService slugService;

    public ProductService(ProductRepository repository, CategoryService categoryService, SlugService slugService) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.slugService = slugService;
    }

    public List<Product> findPublic(String search, String categoryId) {
        if (search != null && !search.isBlank()) return repository.searchActive(search.trim());
        if (categoryId != null && !categoryId.isBlank()) return repository.findByCategoryIdAndActiveTrue(categoryId);
        return repository.findByActiveTrue();
    }

    public List<Product> findAllAdmin() {
        return repository.findAll();
    }

    public Product findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
    }

    public Product findPublicBySlug(String slug) {
        Product p = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
        if (!p.isActive()) throw new ResourceNotFoundException("Produto não encontrado.");
        return p;
    }

    public Product create(CreateProductRequest request) {
        if (repository.existsBySku(request.sku())) throw new BusinessException("SKU já cadastrado.");
        categoryService.findById(request.categoryId());

        String slug = uniqueSlug(request.name(), null);
        validatePromotionalPrice(request.price(), request.promotionalPrice());

        Product product = new Product();
        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setSku(request.sku().trim().toUpperCase());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setPromotionalPrice(request.promotionalPrice());
        product.setCategoryId(request.categoryId());
        product.setMaterial(request.material());
        product.setColor(request.color());
        product.setStock(request.stock());
        product.setMinimumStock(request.minimumStock());
        product.setFeatured(request.featured());
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product);
    }

    public Product update(String id, UpdateProductRequest request) {
        Product product = findById(id);
        categoryService.findById(request.categoryId());

        repository.findBySku(request.sku().trim().toUpperCase())
                .filter(p -> !p.getId().equals(id))
                .ifPresent(p -> { throw new BusinessException("SKU já cadastrado."); });

        validatePromotionalPrice(request.price(), request.promotionalPrice());

        product.setName(request.name().trim());
        product.setSlug(uniqueSlug(request.name(), id));
        product.setSku(request.sku().trim().toUpperCase());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setPromotionalPrice(request.promotionalPrice());
        product.setCategoryId(request.categoryId());
        product.setMaterial(request.material());
        product.setColor(request.color());
        product.setMinimumStock(request.minimumStock());
        product.setFeatured(request.featured());
        product.setActive(request.active());
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product);
    }

    public Product addImage(String id, ProductImageRequest request) {
        Product product = findById(id);
        if (request.main()) product.getImages().forEach(i -> i.setMain(false));
        product.getImages().add(new ProductImage(request.url(), request.publicId(), request.main()));
        if (product.getImages().size() == 1) product.getImages().getFirst().setMain(true);
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product);
    }

    public Product removeImage(String id, String publicId) {
        Product product = findById(id);
        product.getImages().removeIf(i -> i.getPublicId().equals(publicId));
        if (!product.getImages().isEmpty() && product.getImages().stream().noneMatch(ProductImage::isMain)) {
            product.getImages().getFirst().setMain(true);
        }
        product.setUpdatedAt(LocalDateTime.now());
        return repository.save(product);
    }

    public Product setActive(String id, boolean active) {
        Product p = findById(id);
        p.setActive(active);
        p.setUpdatedAt(LocalDateTime.now());
        return repository.save(p);
    }

    public BigDecimal currentPrice(Product product) {
        return product.getPromotionalPrice() != null ? product.getPromotionalPrice() : product.getPrice();
    }

    private void validatePromotionalPrice(BigDecimal price, BigDecimal promotional) {
        if (promotional != null && promotional.compareTo(price) >= 0) {
            throw new BusinessException("O preço promocional deve ser menor que o preço normal.");
        }
    }

    private String uniqueSlug(String name, String currentId) {
        String base = slugService.slugify(name);
        String slug = base;
        int suffix = 2;
        while (true) {
            var found = repository.findBySlug(slug);
            if (found.isEmpty() || found.get().getId().equals(currentId)) return slug;
            slug = base + "-" + suffix++;
        }
    }
}
