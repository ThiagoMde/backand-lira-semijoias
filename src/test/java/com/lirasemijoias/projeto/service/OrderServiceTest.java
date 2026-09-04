package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.order.*;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.InsufficientStockException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.*;
import com.lirasemijoias.projeto.model.enums.DeliveryType;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductService productService;
    private StockService stockService;
    private WhatsappService whatsappService;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productService = mock(ProductService.class);
        stockService = mock(StockService.class);
        whatsappService = mock(WhatsappService.class);
        service = new OrderService(orderRepository, productService, stockService, whatsappService);
    }

    @Test
    void checkoutMergesRepeatedProductsBuildsOrderAndWhatsappResponse() {
        Product ring = product("p1", "Anel", "SKU-A");
        Product earrings = product("p2", "Brinco", "SKU-B");
        when(productService.findById("p1")).thenReturn(ring);
        when(productService.findById("p2")).thenReturn(earrings);
        when(productService.currentPrice(ring)).thenReturn(new BigDecimal("10.00"));
        when(productService.currentPrice(earrings)).thenReturn(new BigDecimal("7.50"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-1");
            return order;
        });
        when(whatsappService.message(any(Order.class))).thenReturn("checkout-message");
        when(whatsappService.url("checkout-message")).thenReturn("https://wa.me/checkout");

        CheckoutRequest request = new CheckoutRequest(
                new CustomerRequest("Ana", "11999999999"),
                new DeliveryRequest(DeliveryType.DELIVERY,
                        new AddressRequest("Rua A", "10", "Centro", "São Paulo", "SP",
                                "01000-000", "Apto 2")),
                List.of(new OrderItemRequest("p1", 1), new OrderItemRequest("p2", 2),
                        new OrderItemRequest("p1", 3)),
                "Entregar à tarde");

        CheckoutResponse response = service.checkout(request);

        assertAll(
                () -> assertEquals("order-1", response.orderId()),
                () -> assertTrue(response.orderNumber().matches("PED-\\d{8}-\\d{6}-[A-F0-9]{4}")),
                () -> assertEquals(new BigDecimal("55.00"), response.total()),
                () -> assertEquals(OrderStatus.PENDING, response.status()),
                () -> assertEquals("checkout-message", response.whatsappMessage()),
                () -> assertEquals("https://wa.me/checkout", response.whatsappUrl())
        );

        var captor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertAll(
                () -> assertEquals("Ana", saved.getCustomer().getName()),
                () -> assertEquals("11999999999", saved.getCustomer().getPhone()),
                () -> assertEquals(DeliveryType.DELIVERY, saved.getDelivery().getType()),
                () -> assertEquals("Rua A", saved.getDelivery().getAddress().getStreet()),
                () -> assertEquals("10", saved.getDelivery().getAddress().getNumber()),
                () -> assertEquals("Centro", saved.getDelivery().getAddress().getDistrict()),
                () -> assertEquals("São Paulo", saved.getDelivery().getAddress().getCity()),
                () -> assertEquals("SP", saved.getDelivery().getAddress().getState()),
                () -> assertEquals("01000-000", saved.getDelivery().getAddress().getZipCode()),
                () -> assertEquals("Apto 2", saved.getDelivery().getAddress().getComplement()),
                () -> assertEquals(2, saved.getItems().size()),
                () -> assertEquals(new BigDecimal("55.00"), saved.getSubtotal()),
                () -> assertEquals(BigDecimal.ZERO, saved.getDeliveryFee()),
                () -> assertEquals(new BigDecimal("55.00"), saved.getTotal()),
                () -> assertEquals(OrderStatus.PENDING, saved.getStatus()),
                () -> assertEquals("Entregar à tarde", saved.getNotes()),
                () -> assertNotNull(saved.getCreatedAt()),
                () -> assertNotNull(saved.getUpdatedAt())
        );
        OrderItem ringItem = saved.getItems().getFirst();
        assertAll(
                () -> assertEquals("p1", ringItem.getProductId()),
                () -> assertEquals("Anel", ringItem.getName()),
                () -> assertEquals("SKU-A", ringItem.getSku()),
                () -> assertEquals(4, ringItem.getQuantity()),
                () -> assertEquals(new BigDecimal("10.00"), ringItem.getUnitPrice()),
                () -> assertEquals(new BigDecimal("40.00"), ringItem.getSubtotal())
        );
        verify(productService, times(1)).findById("p1");
        verify(stockService).validateAvailable("p1", 4);
        verify(stockService).validateAvailable("p2", 2);
        verify(whatsappService).message(saved);
    }

    @Test
    void checkoutAllowsPickupWithoutAddress() {
        Product product = product("p1", "Anel", "SKU");
        when(productService.findById("p1")).thenReturn(product);
        when(productService.currentPrice(product)).thenReturn(new BigDecimal("20"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(whatsappService.message(any(Order.class))).thenReturn("message");
        when(whatsappService.url("message")).thenReturn("url");
        CheckoutRequest request = checkoutRequest(DeliveryType.PICKUP, null,
                List.of(new OrderItemRequest("p1", 1)));

        CheckoutResponse response = service.checkout(request);

        assertEquals(new BigDecimal("20"), response.total());
        var captor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertNull(captor.getValue().getDelivery().getAddress());
    }

    @Test
    void checkoutRejectsDeliveryWithoutAddressBeforeProductLookup() {
        CheckoutRequest request = checkoutRequest(DeliveryType.DELIVERY, null,
                List.of(new OrderItemRequest("p1", 1)));

        assertThrows(BusinessException.class, () -> service.checkout(request));

        verifyNoInteractions(productService, stockService, orderRepository, whatsappService);
    }

    @Test
    void checkoutDoesNotPersistWhenStockValidationFails() {
        Product product = product("p1", "Anel", "SKU");
        when(productService.findById("p1")).thenReturn(product);
        doThrow(new InsufficientStockException("insufficient"))
                .when(stockService).validateAvailable("p1", 2);

        assertThrows(InsufficientStockException.class, () -> service.checkout(
                checkoutRequest(DeliveryType.PICKUP, null, List.of(new OrderItemRequest("p1", 2)))));

        verify(productService, never()).currentPrice(any());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(whatsappService);
    }

    @Test
    void findAllSelectsStatusSpecificOrUnfilteredQuery() {
        List<Order> all = List.of(order("o1", OrderStatus.PENDING));
        List<Order> delivered = List.of(order("o2", OrderStatus.DELIVERED));
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(all);
        when(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.DELIVERED)).thenReturn(delivered);

        assertSame(all, service.findAll(null));
        assertSame(delivered, service.findAll(OrderStatus.DELIVERED));
    }

    @Test
    void findByIdReturnsOrderOrThrows() {
        Order order = order("o1", OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertSame(order, service.findById("o1"));
        assertThrows(ResourceNotFoundException.class, () -> service.findById("missing"));
    }

    @Test
    void confirmValidatesAllItemsBeforeSellingAndPersistsConfirmedOrder() {
        Order order = orderWithItems("o1", OrderStatus.PENDING,
                item("p1", 2), item("p2", 1));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = service.confirm("o1");

        assertSame(order, result);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertNotNull(order.getUpdatedAt());
        InOrder inOrder = inOrder(stockService, orderRepository);
        inOrder.verify(stockService).validateAvailable("p1", 2);
        inOrder.verify(stockService).validateAvailable("p2", 1);
        inOrder.verify(stockService).sale("p1", 2, "o1");
        inOrder.verify(stockService).sale("p2", 1, "o1");
        inOrder.verify(orderRepository).save(order);
    }

    @Test
    void confirmRejectsOrderThatIsNotPending() {
        Order order = order("o1", OrderStatus.CONFIRMED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> service.confirm("o1"));
        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmDoesNotSellAnyItemWhenValidationFails() {
        Order order = orderWithItems("o1", OrderStatus.PENDING,
                item("p1", 2), item("p2", 3));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        doThrow(new InsufficientStockException("insufficient"))
                .when(stockService).validateAvailable("p2", 3);

        assertThrows(InsufficientStockException.class, () -> service.confirm("o1"));

        verify(stockService, never()).sale(anyString(), anyInt(), anyString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelIsIdempotentForAlreadyCancelledOrder() {
        Order order = order("o1", OrderStatus.CANCELLED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        assertSame(order, service.cancel("o1"));
        verifyNoInteractions(stockService);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelRejectsShippedAndDeliveredOrders() {
        for (OrderStatus status : List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED)) {
            reset(orderRepository, stockService);
            Order order = order("o1", status);
            when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

            assertThrows(BusinessException.class, () -> service.cancel("o1"));
            verifyNoInteractions(stockService);
            verify(orderRepository, never()).save(any());
        }
    }

    @Test
    void cancelConfirmedOrderRestoresEveryItemAndPersists() {
        Order order = orderWithItems("o1", OrderStatus.CONFIRMED,
                item("p1", 2), item("p2", 1));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = service.cancel("o1");

        assertSame(order, result);
        verify(stockService).returnToStock("p1", 2, "o1");
        verify(stockService).returnToStock("p2", 1, "o1");
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getUpdatedAt());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelPendingOrderDoesNotRestoreStock() {
        Order order = orderWithItems("o1", OrderStatus.PENDING, item("p1", 2));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        service.cancel("o1");

        verifyNoInteractions(stockService);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void updateStatusToConfirmedUsesConfirmationFlow() {
        Order order = orderWithItems("o1", OrderStatus.PENDING, item("p1", 2));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = service.updateStatus("o1", OrderStatus.CONFIRMED);

        assertSame(order, result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository, times(2)).findById("o1");
        verify(stockService).sale("p1", 2, "o1");
    }

    @Test
    void updateStatusToCancelledUsesCancellationFlow() {
        Order order = orderWithItems("o1", OrderStatus.PAID, item("p1", 1));
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = service.updateStatus("o1", OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository, times(2)).findById("o1");
        verify(stockService).returnToStock("p1", 1, "o1");
    }

    @Test
    void updateStatusRejectsSkippingConfirmationOrChangingCancelledOrder() {
        Order pending = order("pending", OrderStatus.PENDING);
        Order cancelled = order("cancelled", OrderStatus.CANCELLED);
        when(orderRepository.findById("pending")).thenReturn(Optional.of(pending));
        when(orderRepository.findById("cancelled")).thenReturn(Optional.of(cancelled));

        assertThrows(BusinessException.class,
                () -> service.updateStatus("pending", OrderStatus.PAID));
        assertThrows(BusinessException.class,
                () -> service.updateStatus("cancelled", OrderStatus.READY));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatusPersistsAllowedTransition() {
        Order order = order("o1", OrderStatus.CONFIRMED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = service.updateStatus("o1", OrderStatus.PREPARING);

        assertSame(order, result);
        assertEquals(OrderStatus.PREPARING, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(orderRepository).save(order);
    }

    private static CheckoutRequest checkoutRequest(DeliveryType type, AddressRequest address,
                                                   List<OrderItemRequest> items) {
        return new CheckoutRequest(new CustomerRequest("Ana", "11999999999"),
                new DeliveryRequest(type, address), items, null);
    }

    private static Product product(String id, String name, String sku) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku(sku);
        return product;
    }

    private static Order order(String id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        return order;
    }

    private static Order orderWithItems(String id, OrderStatus status, OrderItem... items) {
        Order order = order(id, status);
        order.setItems(List.of(items));
        return order;
    }

    private static OrderItem item(String productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
