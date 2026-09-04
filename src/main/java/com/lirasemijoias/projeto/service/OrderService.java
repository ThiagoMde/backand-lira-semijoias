package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.dto.order.*;
import com.lirasemijoias.projeto.exception.BusinessException;
import com.lirasemijoias.projeto.exception.ResourceNotFoundException;
import com.lirasemijoias.projeto.model.*;
import com.lirasemijoias.projeto.model.enums.DeliveryType;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final WhatsappService whatsappService;

    public OrderService(OrderRepository orderRepository, ProductService productService,
                        StockService stockService, WhatsappService whatsappService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.stockService = stockService;
        this.whatsappService = whatsappService;
    }

    public CheckoutResponse checkout(CheckoutRequest request) {
        validateDelivery(request.delivery());

        Map<String, Integer> requestedQuantities = new LinkedHashMap<>();
        for (OrderItemRequest requested : request.items()) {
            requestedQuantities.merge(requested.productId(), requested.quantity(), Integer::sum);
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> requested : requestedQuantities.entrySet()) {
            Product product = productService.findById(requested.getKey());
            int quantity = requested.getValue();
            stockService.validateAvailable(product.getId(), quantity);

            BigDecimal unitPrice = productService.currentPrice(product);
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setName(product.getName());
            item.setSku(product.getSku());
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity);
            item.setSubtotal(itemSubtotal);
            items.add(item);

            subtotal = subtotal.add(itemSubtotal);
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(new Customer(request.customer().name(), request.customer().phone()));
        order.setDelivery(toDelivery(request.delivery()));
        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setTotal(subtotal);
        order.setStatus(OrderStatus.PENDING);
        order.setNotes(request.notes());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        String message = whatsappService.message(order);
        return new CheckoutResponse(order.getId(), order.getOrderNumber(), order.getTotal(),
                order.getStatus(), message, whatsappService.url(message));
    }

    public List<Order> findAll(OrderStatus status) {
        return status == null ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Order findById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
    }

    @Transactional
    public Order confirm(String id) {
        Order order = findById(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Somente pedidos PENDING podem ser confirmados.");
        }

        for (OrderItem item : order.getItems()) {
            stockService.validateAvailable(item.getProductId(), item.getQuantity());
        }
        for (OrderItem item : order.getItems()) {
            stockService.sale(item.getProductId(), item.getQuantity(), order.getId());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancel(String id) {
        Order order = findById(id);
        if (order.getStatus() == OrderStatus.CANCELLED) return order;
        if (EnumSet.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED).contains(order.getStatus())) {
            throw new BusinessException("Pedido enviado/entregue não pode ser cancelado por esta operação.");
        }

        boolean restore = EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY)
                .contains(order.getStatus());
        if (restore) {
            for (OrderItem item : order.getItems()) {
                stockService.returnToStock(item.getProductId(), item.getQuantity(), order.getId());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateStatus(String id, OrderStatus newStatus) {
        Order order = findById(id);
        if (newStatus == OrderStatus.CONFIRMED) return confirm(id);
        if (newStatus == OrderStatus.CANCELLED) return cancel(id);
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new BusinessException("Confirme o pedido antes de alterar para outro status.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Pedido cancelado não pode mudar de status.");
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private void validateDelivery(DeliveryRequest request) {
        if (request.type() == DeliveryType.DELIVERY && request.address() == null) {
            throw new BusinessException("Endereço é obrigatório para entrega.");
        }
    }

    private Delivery toDelivery(DeliveryRequest request) {
        Delivery delivery = new Delivery();
        delivery.setType(request.type());

        if (request.address() != null) {
            DeliveryAddress address = new DeliveryAddress();
            address.setStreet(request.address().street());
            address.setNumber(request.address().number());
            address.setDistrict(request.address().district());
            address.setCity(request.address().city());
            address.setState(request.address().state());
            address.setZipCode(request.address().zipCode());
            address.setComplement(request.address().complement());
            delivery.setAddress(address);
        }
        return delivery;
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        return "PED-" + date + "-" + random;
    }
}
