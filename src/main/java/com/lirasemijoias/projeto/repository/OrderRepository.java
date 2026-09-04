package com.lirasemijoias.projeto.repository;

import com.lirasemijoias.projeto.model.Order;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByStatus(OrderStatus status);
}
