package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.repository.OrderRepository;
import com.lirasemijoias.projeto.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    @Test
    void getAggregatesProductsStockAndOrderCounters() {
        ProductRepository products = mock(ProductRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        Product active = product(true, 5);
        Product activeOut = product(true, 0);
        Product inactive = product(false, 3);
        when(products.findAll()).thenReturn(List.of(active, activeOut, inactive));
        when(products.findLowStock()).thenReturn(List.of(activeOut, inactive));
        when(orders.countByStatus(OrderStatus.PENDING)).thenReturn(4L);
        when(orders.countByStatus(OrderStatus.CONFIRMED)).thenReturn(2L);
        when(orders.countByStatus(OrderStatus.DELIVERED)).thenReturn(7L);

        Map<String, Object> result = new DashboardService(products, orders).get();

        assertEquals(List.of("productsTotal", "productsActive", "stockUnits", "lowStock",
                "outOfStock", "ordersPending", "ordersConfirmed", "ordersDelivered"),
                List.copyOf(result.keySet()));
        assertEquals(3, result.get("productsTotal"));
        assertEquals(2L, result.get("productsActive"));
        assertEquals(8L, result.get("stockUnits"));
        assertEquals(2, result.get("lowStock"));
        assertEquals(1L, result.get("outOfStock"));
        assertEquals(4L, result.get("ordersPending"));
        assertEquals(2L, result.get("ordersConfirmed"));
        assertEquals(7L, result.get("ordersDelivered"));
    }

    private static Product product(boolean active, int stock) {
        Product product = new Product();
        product.setActive(active);
        product.setStock(stock);
        return product;
    }
}
