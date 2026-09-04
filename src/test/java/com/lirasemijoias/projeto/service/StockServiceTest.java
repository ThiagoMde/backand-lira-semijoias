package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.exception.InsufficientStockException;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.StockMovement;
import com.lirasemijoias.projeto.model.enums.StockMovementType;
import com.lirasemijoias.projeto.repository.ProductRepository;
import com.lirasemijoias.projeto.repository.StockMovementRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StockServiceTest {

    private ProductService productService;
    private ProductRepository productRepository;
    private StockMovementRepository movementRepository;
    private MongoTemplate mongoTemplate;
    private StockService service;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productRepository = mock(ProductRepository.class);
        movementRepository = mock(StockMovementRepository.class);
        mongoTemplate = mock(MongoTemplate.class);
        service = new StockService(productService, productRepository, movementRepository, mongoTemplate);
    }

    @Test
    void entryAtomicallyAddsStockAndRecordsMovement() {
        Product updated = product("p1", "Anel", 8, true);
        mockAtomicUpdate(updated);

        Product result = service.entry("p1", 3, "Reposição", "user-1");

        assertSame(updated, result);
        AtomicArguments arguments = captureAtomicArguments();
        assertEquals("p1", arguments.query().getQueryObject().get("_id"));
        assertFalse(arguments.query().getQueryObject().containsKey("stock"));
        assertEquals(3, updateValue(arguments.update(), "$inc", "stock"));
        assertNotNull(updateValue(arguments.update(), "$set", "updatedAt"));

        StockMovement movement = captureMovement();
        assertAll(
                () -> assertEquals("p1", movement.getProductId()),
                () -> assertEquals(StockMovementType.ENTRY, movement.getType()),
                () -> assertEquals(3, movement.getQuantity()),
                () -> assertEquals(5, movement.getStockBefore()),
                () -> assertEquals(8, movement.getStockAfter()),
                () -> assertEquals("Reposição", movement.getReason()),
                () -> assertEquals("user-1", movement.getUserId()),
                () -> assertNull(movement.getOrderId()),
                () -> assertNotNull(movement.getCreatedAt())
        );
    }

    @Test
    void exitAtomicallyRequiresEnoughStockAndRecordsNegativeDelta() {
        Product updated = product("p1", "Anel", 6, true);
        mockAtomicUpdate(updated);

        service.exit("p1", 4, "Avaria", "user-2");

        AtomicArguments arguments = captureAtomicArguments();
        Document stockCriteria = (Document) arguments.query().getQueryObject().get("stock");
        assertEquals(4, stockCriteria.get("$gte"));
        assertEquals(-4, updateValue(arguments.update(), "$inc", "stock"));

        StockMovement movement = captureMovement();
        assertEquals(StockMovementType.EXIT, movement.getType());
        assertEquals(10, movement.getStockBefore());
        assertEquals(6, movement.getStockAfter());
        assertEquals("Avaria", movement.getReason());
        assertEquals("user-2", movement.getUserId());
    }

    @Test
    void saleSubtractsStockAndAssociatesOrder() {
        Product updated = product("p1", "Anel", 7, true);
        mockAtomicUpdate(updated);

        service.sale("p1", 2, "order-1");

        StockMovement movement = captureMovement();
        assertAll(
                () -> assertEquals(StockMovementType.SALE, movement.getType()),
                () -> assertEquals(9, movement.getStockBefore()),
                () -> assertEquals(7, movement.getStockAfter()),
                () -> assertEquals("order-1", movement.getOrderId()),
                () -> assertNull(movement.getUserId()),
                () -> assertTrue(movement.getReason().contains("order-1"))
        );
    }

    @Test
    void returnToStockAddsStockAndAssociatesOrder() {
        Product updated = product("p1", "Anel", 10, true);
        mockAtomicUpdate(updated);

        service.returnToStock("p1", 2, "order-2");

        AtomicArguments arguments = captureAtomicArguments();
        assertFalse(arguments.query().getQueryObject().containsKey("stock"));
        assertEquals(2, updateValue(arguments.update(), "$inc", "stock"));
        StockMovement movement = captureMovement();
        assertEquals(StockMovementType.RETURN, movement.getType());
        assertEquals(8, movement.getStockBefore());
        assertEquals(10, movement.getStockAfter());
        assertEquals("order-2", movement.getOrderId());
    }

    @Test
    void movementsRejectZeroAndNegativeQuantityBeforeDatabaseCall() {
        assertThrows(IllegalArgumentException.class,
                () -> service.entry("p1", 0, "reason", "user"));
        assertThrows(IllegalArgumentException.class,
                () -> service.exit("p1", -1, "reason", "user"));

        verifyNoInteractions(mongoTemplate, movementRepository);
    }

    @Test
    void failedAtomicSubtractionReportsInsufficientStockAndDoesNotRecordMovement() {
        Product existing = product("p1", "Anel", 1, true);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Product.class))).thenReturn(null);
        when(productService.findById("p1")).thenReturn(existing);

        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> service.sale("p1", 2, "order-1"));

        assertTrue(exception.getMessage().contains("Anel"));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void validateAvailableAcceptsActiveProductWithEnoughStock() {
        when(productService.findById("p1")).thenReturn(product("p1", "Anel", 3, true));

        assertDoesNotThrow(() -> service.validateAvailable("p1", 3));
    }

    @Test
    void validateAvailableRejectsInactiveProductBeforeQuantityCheck() {
        when(productService.findById("p1")).thenReturn(product("p1", "Anel", 100, false));

        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> service.validateAvailable("p1", 1));
        assertTrue(exception.getMessage().contains("Anel"));
    }

    @Test
    void validateAvailableRejectsQuantityAboveStock() {
        when(productService.findById("p1")).thenReturn(product("p1", "Anel", 2, true));

        assertThrows(InsufficientStockException.class,
                () -> service.validateAvailable("p1", 3));
    }

    @Test
    void queryMethodsDelegateAndHistoryByProductValidatesProduct() {
        List<Product> low = List.of(product("p1", "Anel", 1, true));
        List<StockMovement> allHistory = List.of(new StockMovement());
        List<StockMovement> productHistory = List.of(new StockMovement(), new StockMovement());
        when(productRepository.findLowStock()).thenReturn(low);
        when(movementRepository.findAllByOrderByCreatedAtDesc()).thenReturn(allHistory);
        when(movementRepository.findByProductIdOrderByCreatedAtDesc("p1")).thenReturn(productHistory);
        when(productService.findById("p1")).thenReturn(low.getFirst());

        assertSame(low, service.lowStock());
        assertSame(allHistory, service.history());
        assertSame(productHistory, service.historyByProduct("p1"));
        verify(productService).findById("p1");
    }

    private void mockAtomicUpdate(Product updated) {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Product.class))).thenReturn(updated);
    }

    private AtomicArguments captureAtomicArguments() {
        var queryCaptor = org.mockito.ArgumentCaptor.forClass(Query.class);
        var updateCaptor = org.mockito.ArgumentCaptor.forClass(Update.class);
        var optionsCaptor = org.mockito.ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongoTemplate).findAndModify(queryCaptor.capture(), updateCaptor.capture(),
                optionsCaptor.capture(), eq(Product.class));
        assertTrue(optionsCaptor.getValue().isReturnNew());
        return new AtomicArguments(queryCaptor.getValue(), updateCaptor.getValue());
    }

    private StockMovement captureMovement() {
        var captor = org.mockito.ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Object updateValue(Update update, String operator, String field) {
        Document operation = (Document) update.getUpdateObject().get(operator);
        return operation.get(field);
    }

    private static Product product(String id, String name, int stock, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setStock(stock);
        product.setActive(active);
        return product;
    }

    private record AtomicArguments(Query query, Update update) {}
}
