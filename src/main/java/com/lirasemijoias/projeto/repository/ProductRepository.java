package com.lirasemijoias.projeto.repository;

import com.lirasemijoias.projeto.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findBySlug(String slug);
    boolean existsBySku(String sku);
    boolean existsBySlug(String slug);
    List<Product> findByActiveTrue();
    List<Product> findByCategoryIdAndActiveTrue(String categoryId);

    @Query("{ 'active': true, '$or': [ { 'name': { $regex: ?0, $options: 'i' } }, { 'sku': { $regex: ?0, $options: 'i' } } ] }")
    List<Product> searchActive(String term);

    @Query("{ 'active': true, $expr: { $lte: ['$stock', '$minimumStock'] } }")
    List<Product> findLowStock();
}
