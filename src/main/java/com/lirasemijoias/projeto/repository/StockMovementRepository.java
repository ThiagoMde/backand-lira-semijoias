package com.lirasemijoias.projeto.repository;

import com.lirasemijoias.projeto.model.StockMovement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StockMovementRepository extends MongoRepository<StockMovement, String> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(String productId);
    List<StockMovement> findAllByOrderByCreatedAtDesc();
}
