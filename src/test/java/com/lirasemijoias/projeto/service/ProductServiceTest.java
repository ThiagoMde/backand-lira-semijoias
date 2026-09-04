package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.product.CreateProductRequest;
import com.lirasemijoias.projeto.dto.product.ProductImageRequest;
import com.lirasemijoias.projeto.dto.product.UpdateProductRequest;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.Category;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.ProductImage;
import com.lirasemijoias.projeto.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private ProductRepository repository;
    private CategoryService categoryService;
    private SlugService slugService;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        categoryService = mock(CategoryService.class);
        slugService = mock(SlugService.class);
        service = new ProductService(repository, categoryService, slugService);
    }

    @Test
    void findPublicPrioritizesSearchThenCategoryThenAllActive() {
        List<Product> searched = List.of(product("s"));
        List<Product> category = List.of(product("c"));
        List<Product> all = List.of(product("a"));
        when(repository.searchActive("anel")).thenReturn(searched);
        when(repository.findByCategoryIdAndActiveTrue("cat-1")).thenReturn(category);
        when(repository.findByActiveTrue()).thenReturn(all);

        assertSame(searched, service.findPublic("  anel  ", "cat-1"));
        assertSame(category, service.findPublic("   ", "cat-1"));
        assertSame(all, service.findPublic(null, "   "));
        assertSame(all, service.findPublic(null, null));

        verify(repository).searchActive("anel");
        verify(repository).findByCategoryIdAndActiveTrue("cat-1");
        verify(repository, times(2)).findByActiveTrue();
    }

    @Test
    void findAllAdminDelegatesToRepository() {
        List<Product> products = List.of(product("p1"), product("p2"));
        when(repository.findAll()).thenReturn(products);
        assertSame(products, service.findAllAdmin());
    }

    @Test
    void findByIdReturnsProductOrThrows() {
        Product product = product("p1");
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertSame(product, service.findById("p1"));
        assertThrows(ResourceNotFoundException.class, () -> service.findById("missing"));
    }

    @Test
    void findPublicBySlugReturnsOnlyActiveProduct() {
        Product active = product("active");
        active.setActive(true);
        Product inactive = product("inactive");
        inactive.setActive(false);
        when(repository.findBySlug("active")).thenReturn(Optional.of(active));
        when(repository.findBySlug("inactive")).thenReturn(Optional.of(inactive));
        when(repository.findBySlug("missing")).thenReturn(Optional.empty());

        assertSame(active, service.findPublicBySlug("active"));
        assertThrows(ResourceNotFoundException.class, () -> service.findPublicBySlug("inactive"));
        assertThrows(ResourceNotFoundException.class, () -> service.findPublicBySlug("missing"));
    }

    @Test
    void createNormalizesFieldsUsesUniqueSlugAndSetsDefaults() {
        CreateProductRequest request = createRequest(new BigDecimal("100.00"), new BigDecimal("79.90"));
        when(categoryService.findById("cat-1")).thenReturn(new Category());
        when(slugService.slugify(request.name())).thenReturn("anel-luz");
        Product collision = product("other");
        when(repository.findBySlug("anel-luz")).thenReturn(Optional.of(collision));
        when(repository.findBySlug("anel-luz-2")).thenReturn(Optional.empty());
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        Product result = service.create(request);

        assertAll(
                () -> assertEquals("Anel Luz", result.getName()),
                () -> assertEquals("anel-luz-2", result.getSlug()),
                () -> assertEquals("SKU-1", result.getSku()),
                () -> assertEquals("Descrição", result.getDescription()),
                () -> assertEquals(new BigDecimal("100.00"), result.getPrice()),
                () -> assertEquals(new BigDecimal("79.90"), result.getPromotionalPrice()),
                () -> assertEquals("cat-1", result.getCategoryId()),
                () -> assertEquals("Ouro", result.getMaterial()),
                () -> assertEquals("Dourado", result.getColor()),
                () -> assertEquals(7, result.getStock()),
                () -> assertEquals(2, result.getMinimumStock()),
                () -> assertTrue(result.isFeatured()),
                () -> assertTrue(result.isActive()),
                () -> assertFalse(result.getCreatedAt().isBefore(before)),
                () -> assertNotNull(result.getUpdatedAt())
        );
        verify(repository).existsBySku(" sku-1 ");
        verify(categoryService).findById("cat-1");
    }

    @Test
    void createRejectsDuplicateSkuBeforeCheckingCategory() {
        CreateProductRequest request = createRequest(new BigDecimal("100"), null);
        when(repository.existsBySku(request.sku())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(request));

        verifyNoInteractions(categoryService, slugService);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsPromotionalPriceEqualToOrAboveNormalPrice() {
        CreateProductRequest equal = createRequest(new BigDecimal("100"), new BigDecimal("100"));
        CreateProductRequest above = createRequest(new BigDecimal("100"), new BigDecimal("101"));
        when(slugService.slugify(anyString())).thenReturn("anel-luz");
        when(repository.findBySlug("anel-luz")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.create(equal));
        assertThrows(BusinessException.class, () -> service.create(above));
        verify(repository, never()).save(any());
    }

    @Test
    void createAcceptsMissingPromotionalPrice() {
        CreateProductRequest request = createRequest(new BigDecimal("100"), null);
        when(slugService.slugify(request.name())).thenReturn("anel-luz");
        when(repository.findBySlug("anel-luz")).thenReturn(Optional.empty());
        when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = service.create(request);

        assertNull(result.getPromotionalPrice());
        verify(repository).save(result);
    }

    @Test
    void updateChangesEditableFieldsKeepsStockAndAllowsOwnSkuAndSlug() {
        Product existing = product("p1");
        existing.setStock(12);
        Product ownSku = product("p1");
        Product ownSlug = product("p1");
        UpdateProductRequest request = updateRequest(new BigDecimal("120"), new BigDecimal("99"));
        when(repository.findById("p1")).thenReturn(Optional.of(existing));
        when(repository.findBySku("NEW-SKU")).thenReturn(Optional.of(ownSku));
        when(slugService.slugify(request.name())).thenReturn("novo-produto");
        when(repository.findBySlug("novo-produto")).thenReturn(Optional.of(ownSlug));
        when(repository.save(existing)).thenReturn(existing);

        Product result = service.update("p1", request);

        assertSame(existing, result);
        assertAll(
                () -> assertEquals("Novo Produto", existing.getName()),
                () -> assertEquals("novo-produto", existing.getSlug()),
                () -> assertEquals("NEW-SKU", existing.getSku()),
                () -> assertEquals("Nova descrição", existing.getDescription()),
                () -> assertEquals(new BigDecimal("120"), existing.getPrice()),
                () -> assertEquals(new BigDecimal("99"), existing.getPromotionalPrice()),
                () -> assertEquals("cat-2", existing.getCategoryId()),
                () -> assertEquals(12, existing.getStock()),
                () -> assertEquals(4, existing.getMinimumStock()),
                () -> assertFalse(existing.isFeatured()),
                () -> assertFalse(existing.isActive()),
                () -> assertNotNull(existing.getUpdatedAt())
        );
    }

    @Test
    void updateRejectsSkuOwnedByAnotherProduct() {
        Product existing = product("p1");
        when(repository.findById("p1")).thenReturn(Optional.of(existing));
        when(repository.findBySku("NEW-SKU")).thenReturn(Optional.of(product("p2")));

        assertThrows(BusinessException.class,
                () -> service.update("p1", updateRequest(new BigDecimal("100"), null)));

        verify(repository, never()).save(any());
        verifyNoInteractions(slugService);
    }

    @Test
    void updateRejectsInvalidPromotionalPrice() {
        Product existing = product("p1");
        when(repository.findById("p1")).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class,
                () -> service.update("p1", updateRequest(new BigDecimal("100"), new BigDecimal("100"))));
        verify(repository, never()).save(any());
    }

    @Test
    void addFirstImageMakesItMainEvenWhenRequestSaysFalse() {
        Product product = product("p1");
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        Product result = service.addImage("p1", new ProductImageRequest("url", "img-1", false));

        assertSame(product, result);
        assertEquals(1, product.getImages().size());
        assertTrue(product.getImages().getFirst().isMain());
        assertNotNull(product.getUpdatedAt());
    }

    @Test
    void addingMainImageDemotesExistingMainImage() {
        Product product = product("p1");
        ProductImage old = new ProductImage("old-url", "old", true);
        product.getImages().add(old);
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        service.addImage("p1", new ProductImageRequest("new-url", "new", true));

        assertFalse(old.isMain());
        assertTrue(product.getImages().get(1).isMain());
    }

    @Test
    void removeMainImagePromotesFirstRemainingImage() {
        Product product = product("p1");
        product.getImages().add(new ProductImage("main", "main-id", true));
        product.getImages().add(new ProductImage("second", "second-id", false));
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        service.removeImage("p1", "main-id");

        assertEquals(1, product.getImages().size());
        assertEquals("second-id", product.getImages().getFirst().getPublicId());
        assertTrue(product.getImages().getFirst().isMain());
    }

    @Test
    void removeLastImageLeavesEmptyList() {
        Product product = product("p1");
        product.getImages().add(new ProductImage("url", "only", true));
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        service.removeImage("p1", "only");

        assertTrue(product.getImages().isEmpty());
    }

    @Test
    void removeUnknownImageKeepsExistingMainImage() {
        Product product = product("p1");
        ProductImage main = new ProductImage("url", "main", true);
        product.getImages().add(main);
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        service.removeImage("p1", "unknown");

        assertEquals(1, product.getImages().size());
        assertTrue(main.isMain());
    }

    @Test
    void setActiveUpdatesProductAndCurrentPricePrefersPromotion() {
        Product product = product("p1");
        product.setPrice(new BigDecimal("100"));
        product.setPromotionalPrice(new BigDecimal("80"));
        when(repository.findById("p1")).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        assertSame(product, service.setActive("p1", false));
        assertFalse(product.isActive());
        assertNotNull(product.getUpdatedAt());
        assertEquals(new BigDecimal("80"), service.currentPrice(product));

        product.setPromotionalPrice(null);
        assertEquals(new BigDecimal("100"), service.currentPrice(product));
    }

    private static CreateProductRequest createRequest(BigDecimal price, BigDecimal promotionalPrice) {
        return new CreateProductRequest("  Anel Luz  ", " sku-1 ", "  Descrição  ", price,
                promotionalPrice, "cat-1", "Ouro", "Dourado", 7, 2, true);
    }

    private static UpdateProductRequest updateRequest(BigDecimal price, BigDecimal promotionalPrice) {
        return new UpdateProductRequest("  Novo Produto  ", " new-sku ", "  Nova descrição  ", price,
                promotionalPrice, "cat-2", "Prata", "Prateado", 4, false, false);
    }

    private static Product product(String id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        return product;
    }
}
