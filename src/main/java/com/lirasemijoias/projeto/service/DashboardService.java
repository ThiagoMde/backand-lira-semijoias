package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.repository.OrderRepository;
import com.lirasemijoias.projeto.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DashboardService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> get() {
        var products = productRepository.findAll();
        long active = products.stream().filter(Product::isActive).count();
        long out = products.stream().filter(p -> p.getStock() == 0).count();
        long totalUnits = products.stream().mapToLong(Product::getStock).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productsTotal", products.size());
        result.put("productsActive", active);
        result.put("stockUnits", totalUnits);
        result.put("lowStock", productRepository.findLowStock().size());
        result.put("outOfStock", out);
        result.put("ordersPending", orderRepository.countByStatus(OrderStatus.PENDING));
        result.put("ordersConfirmed", orderRepository.countByStatus(OrderStatus.CONFIRMED));
        result.put("ordersDelivered", orderRepository.countByStatus(OrderStatus.DELIVERED));
        return result;
    }
}
