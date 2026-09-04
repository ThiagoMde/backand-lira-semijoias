package com.lirasemijoias.projeto.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lirasemijoias.projeto.model.enums.DeliveryType;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.model.enums.StockMovementType;
import com.lirasemijoias.projeto.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelBehaviorTest {

    @Test
    void productStartsActiveWithItsOwnEmptyImageList() {
        Product first = new Product();
        Product second = new Product();

        assertAll(
                () -> assertTrue(first.isActive()),
                () -> assertTrue(first.getImages().isEmpty()),
                () -> assertNotSame(first.getImages(), second.getImages())
        );
    }

    @Test
    void productKeepsProvidedImagesAndReplacesNullWithAnEmptyList() {
        Product product = new Product();
        List<ProductImage> images = new ArrayList<>(List.of(
                new ProductImage("https://cdn.exemplo.com/anel.jpg", "produtos/anel", true)));

        product.setImages(images);
        assertSame(images, product.getImages());

        product.setImages(null);
        assertTrue(product.getImages().isEmpty());
    }

    @Test
    void productRetainsCatalogAndInventoryState() {
        Product product = new Product();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 10, 30);
        LocalDateTime updatedAt = createdAt.plusDays(1);
        BigDecimal price = new BigDecimal("129.90");
        BigDecimal promotionalPrice = new BigDecimal("99.90");

        product.setId("product-1");
        product.setName("Colar");
        product.setSlug("colar");
        product.setSku("CO-001");
        product.setDescription("Colar folheado");
        product.setPrice(price);
        product.setPromotionalPrice(promotionalPrice);
        product.setCategoryId("category-1");
        product.setMaterial("Prata");
        product.setColor("Dourado");
        product.setStock(12);
        product.setMinimumStock(3);
        product.setFeatured(true);
        product.setActive(false);
        product.setCreatedAt(createdAt);
        product.setUpdatedAt(updatedAt);

        assertAll(
                () -> assertEquals("product-1", product.getId()),
                () -> assertEquals("Colar", product.getName()),
                () -> assertEquals("colar", product.getSlug()),
                () -> assertEquals("CO-001", product.getSku()),
                () -> assertEquals("Colar folheado", product.getDescription()),
                () -> assertEquals(price, product.getPrice()),
                () -> assertEquals(promotionalPrice, product.getPromotionalPrice()),
                () -> assertEquals("category-1", product.getCategoryId()),
                () -> assertEquals("Prata", product.getMaterial()),
                () -> assertEquals("Dourado", product.getColor()),
                () -> assertEquals(12, product.getStock()),
                () -> assertEquals(3, product.getMinimumStock()),
                () -> assertTrue(product.isFeatured()),
                () -> assertFalse(product.isActive()),
                () -> assertEquals(createdAt, product.getCreatedAt()),
                () -> assertEquals(updatedAt, product.getUpdatedAt())
        );
    }

    @Test
    void orderStartsWithItsOwnEmptyItemListAndRetainsCheckoutSnapshot() {
        Order order = new Order();
        Order anotherOrder = new Order();
        Customer customer = new Customer("Ana", "11999999999");
        Delivery delivery = new Delivery();
        List<OrderItem> items = List.of(orderItem());
        BigDecimal subtotal = new BigDecimal("100.00");
        BigDecimal fee = new BigDecimal("12.00");
        BigDecimal total = new BigDecimal("112.00");
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 14, 0);
        LocalDateTime updatedAt = createdAt.plusMinutes(15);

        assertTrue(order.getItems().isEmpty());
        assertNotSame(order.getItems(), anotherOrder.getItems());

        order.setId("order-1");
        order.setOrderNumber("LIRA-1001");
        order.setCustomer(customer);
        order.setDelivery(delivery);
        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        order.setTotal(total);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setNotes("Presente");
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);

        assertAll(
                () -> assertEquals("order-1", order.getId()),
                () -> assertEquals("LIRA-1001", order.getOrderNumber()),
                () -> assertSame(customer, order.getCustomer()),
                () -> assertSame(delivery, order.getDelivery()),
                () -> assertSame(items, order.getItems()),
                () -> assertEquals(subtotal, order.getSubtotal()),
                () -> assertEquals(fee, order.getDeliveryFee()),
                () -> assertEquals(total, order.getTotal()),
                () -> assertEquals(OrderStatus.CONFIRMED, order.getStatus()),
                () -> assertEquals("Presente", order.getNotes()),
                () -> assertEquals(createdAt, order.getCreatedAt()),
                () -> assertEquals(updatedAt, order.getUpdatedAt())
        );
    }

    @Test
    void orderItemRetainsTheProductSnapshotUsedAtCheckout() {
        OrderItem item = orderItem();

        assertAll(
                () -> assertEquals("product-1", item.getProductId()),
                () -> assertEquals("Brinco", item.getName()),
                () -> assertEquals("BR-001", item.getSku()),
                () -> assertEquals(new BigDecimal("50.00"), item.getUnitPrice()),
                () -> assertEquals(2, item.getQuantity()),
                () -> assertEquals(new BigDecimal("100.00"), item.getSubtotal())
        );
    }

    @Test
    void stockMovementRetainsCompleteAuditInformation() {
        StockMovement movement = new StockMovement();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 4, 9, 15);

        movement.setId("movement-1");
        movement.setProductId("product-1");
        movement.setType(StockMovementType.SALE);
        movement.setQuantity(2);
        movement.setStockBefore(10);
        movement.setStockAfter(8);
        movement.setReason("Pedido confirmado");
        movement.setUserId("user-1");
        movement.setOrderId("order-1");
        movement.setCreatedAt(createdAt);

        assertAll(
                () -> assertEquals("movement-1", movement.getId()),
                () -> assertEquals("product-1", movement.getProductId()),
                () -> assertEquals(StockMovementType.SALE, movement.getType()),
                () -> assertEquals(2, movement.getQuantity()),
                () -> assertEquals(10, movement.getStockBefore()),
                () -> assertEquals(8, movement.getStockAfter()),
                () -> assertEquals("Pedido confirmado", movement.getReason()),
                () -> assertEquals("user-1", movement.getUserId()),
                () -> assertEquals("order-1", movement.getOrderId()),
                () -> assertEquals(createdAt, movement.getCreatedAt())
        );
    }

    @Test
    void categoryStartsActiveAndRetainsCatalogMetadata() {
        Category category = new Category();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 5, 8, 0);
        LocalDateTime updatedAt = createdAt.plusHours(2);

        assertTrue(category.isActive());

        category.setId("category-1");
        category.setName("Aneis");
        category.setSlug("aneis");
        category.setDescription("Colecao de aneis");
        category.setActive(false);
        category.setCreatedAt(createdAt);
        category.setUpdatedAt(updatedAt);

        assertAll(
                () -> assertEquals("category-1", category.getId()),
                () -> assertEquals("Aneis", category.getName()),
                () -> assertEquals("aneis", category.getSlug()),
                () -> assertEquals("Colecao de aneis", category.getDescription()),
                () -> assertFalse(category.isActive()),
                () -> assertEquals(createdAt, category.getCreatedAt()),
                () -> assertEquals(updatedAt, category.getUpdatedAt())
        );
    }

    @Test
    void userStartsActiveRetainsIdentityAndNeverSerializesPassword() throws Exception {
        User user = new User();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 6, 12, 0);
        LocalDateTime updatedAt = createdAt.plusDays(1);

        assertTrue(user.isActive());

        user.setId("user-1");
        user.setName("Administradora");
        user.setEmail("admin@exemplo.com");
        user.setPassword("hash-secreto");
        user.setRole(UserRole.ADMIN);
        user.setActive(false);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(user));

        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertAll(
                () -> assertEquals("user-1", user.getId()),
                () -> assertEquals("Administradora", user.getName()),
                () -> assertEquals("admin@exemplo.com", user.getEmail()),
                () -> assertEquals("hash-secreto", user.getPassword()),
                () -> assertEquals(UserRole.ADMIN, user.getRole()),
                () -> assertFalse(user.isActive()),
                () -> assertEquals(createdAt, user.getCreatedAt()),
                () -> assertEquals(updatedAt, user.getUpdatedAt()),
                () -> assertEquals("admin@exemplo.com", json.get("email").asText()),
                () -> assertFalse(json.has("password"))
        );
    }

    @Test
    void customerAndProductImageConstructorsPopulateTheirValueObjects() {
        Customer customer = new Customer("Beatriz", "11888888888");
        ProductImage image = new ProductImage("https://cdn.exemplo.com/colar.jpg", "produtos/colar", true);

        assertAll(
                () -> assertEquals("Beatriz", customer.getName()),
                () -> assertEquals("11888888888", customer.getPhone()),
                () -> assertEquals("https://cdn.exemplo.com/colar.jpg", image.getUrl()),
                () -> assertEquals("produtos/colar", image.getPublicId()),
                () -> assertTrue(image.isMain())
        );
    }

    @Test
    void simpleValueObjectsAllowDataBindingThroughNoArgConstructors() {
        Customer customer = new Customer();
        customer.setName("Carla");
        customer.setPhone("11777777777");

        ProductImage image = new ProductImage();
        image.setUrl("https://cdn.exemplo.com/pulseira.jpg");
        image.setPublicId("produtos/pulseira");
        image.setMain(false);

        DeliveryAddress address = new DeliveryAddress();
        address.setStreet("Rua Um");
        address.setNumber("10");
        address.setDistrict("Centro");
        address.setCity("Campinas");
        address.setState("SP");
        address.setZipCode("13000-000");
        address.setComplement("Casa");

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setAddress(address);

        assertAll(
                () -> assertEquals("Carla", customer.getName()),
                () -> assertEquals("11777777777", customer.getPhone()),
                () -> assertEquals("https://cdn.exemplo.com/pulseira.jpg", image.getUrl()),
                () -> assertEquals("produtos/pulseira", image.getPublicId()),
                () -> assertFalse(image.isMain()),
                () -> assertEquals("Rua Um", address.getStreet()),
                () -> assertEquals("10", address.getNumber()),
                () -> assertEquals("Centro", address.getDistrict()),
                () -> assertEquals("Campinas", address.getCity()),
                () -> assertEquals("SP", address.getState()),
                () -> assertEquals("13000-000", address.getZipCode()),
                () -> assertEquals("Casa", address.getComplement()),
                () -> assertEquals(DeliveryType.DELIVERY, delivery.getType()),
                () -> assertSame(address, delivery.getAddress())
        );
    }

    private static OrderItem orderItem() {
        OrderItem item = new OrderItem();
        item.setProductId("product-1");
        item.setName("Brinco");
        item.setSku("BR-001");
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("100.00"));
        return item;
    }
}
