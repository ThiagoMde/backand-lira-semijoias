package com.lirasemijoias.projeto.model;

import com.lirasemijoias.projeto.model.enums.StockMovementType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "stockMovements")
public class StockMovement {
    @Id
    private String id;

    private String productId;
    private StockMovementType type;
    private int quantity;
    private int stockBefore;
    private int stockAfter;
    private String reason;
    private String userId;
    private String orderId;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public StockMovementType getType() { return type; }
    public void setType(StockMovementType type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getStockBefore() { return stockBefore; }
    public void setStockBefore(int stockBefore) { this.stockBefore = stockBefore; }
    public int getStockAfter() { return stockAfter; }
    public void setStockAfter(int stockAfter) { this.stockAfter = stockAfter; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
