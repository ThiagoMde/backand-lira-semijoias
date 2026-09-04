package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.exception.InsufficientStockException;
import com.lirasemijoias.projeto.model.Product;
import com.lirasemijoias.projeto.model.StockMovement;
import com.lirasemijoias.projeto.model.enums.StockMovementType;
import com.lirasemijoias.projeto.repository.ProductRepository;
import com.lirasemijoias.projeto.repository.StockMovementRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final MongoTemplate mongoTemplate;

    public StockService(ProductService productService, ProductRepository productRepository,
                        StockMovementRepository movementRepository, MongoTemplate mongoTemplate) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Product entry(String productId, int quantity, String reason, String userId) {
        return apply(productId, quantity, StockMovementType.ENTRY, reason, userId, null, true);
    }

    public Product exit(String productId, int quantity, String reason, String userId) {
        return apply(productId, quantity, StockMovementType.EXIT, reason, userId, null, false);
    }

    public Product sale(String productId, int quantity, String orderId) {
        return apply(productId, quantity, StockMovementType.SALE,
                "Venda do pedido " + orderId, null, orderId, false);
    }

    public Product returnToStock(String productId, int quantity, String orderId) {
        return apply(productId, quantity, StockMovementType.RETURN,
                "Devolução do pedido " + orderId, null, orderId, true);
    }

    public void validateAvailable(String productId, int quantity) {
        Product product = productService.findById(productId);
        if (!product.isActive()) {
            throw new InsufficientStockException("Produto " + product.getName() + " está inativo.");
        }
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Estoque insuficiente para " + product.getName() + ".");
        }
    }

    public List<Product> lowStock() {
        return productRepository.findLowStock();
    }

    public List<StockMovement> history() {
        return movementRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<StockMovement> historyByProduct(String productId) {
        productService.findById(productId);
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    private Product apply(String productId, int quantity, StockMovementType type, String reason,
                          String userId, String orderId, boolean add) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        LocalDateTime now = LocalDateTime.now();
        Query query = Query.query(Criteria.where("_id").is(productId));

        if (!add) {
            query.addCriteria(Criteria.where("stock").gte(quantity));
        }

        int delta = add ? quantity : -quantity;
        Update update = new Update()
                .inc("stock", delta)
                .set("updatedAt", now);

        Product updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class
        );

        if (updated == null) {
            Product existing = productService.findById(productId);
            throw new InsufficientStockException("Estoque insuficiente para " + existing.getName() + ".");
        }

        int after = updated.getStock();
        int before = after - delta;

        StockMovement movement = new StockMovement();
        movement.setProductId(productId);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setStockBefore(before);
        movement.setStockAfter(after);
        movement.setReason(reason);
        movement.setUserId(userId);
        movement.setOrderId(orderId);
        movement.setCreatedAt(now);
        movementRepository.save(movement);

        return updated;
    }
}
