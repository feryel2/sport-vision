package com.sportvision.service;

import com.sportvision.dto.OrderItemDto;
import com.sportvision.dto.OrderRequest;
import com.sportvision.entity.*;
import com.sportvision.repository.OrderRepository;
import com.sportvision.repository.ProductRepository;
import com.sportvision.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        String orderNum = "SV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        order.setOrderNumber(orderNum);
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARTE_BANCAIRE");
        order.setStatus(OrderStatus.CONFIRMED);

        if (request.getUserId() != null) {
            userRepository.findById(request.getUserId()).ifPresent(order::setUser);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable ID : " + itemDto.getProductId()));

            // Vérification et décrémentation de stock
            if (product.getStock() < itemDto.getQuantity()) {
                throw new RuntimeException("Stock insuffisant pour le produit : " + product.getName());
            }
            product.setStock(product.getStock() - itemDto.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem(order, product, itemDto.getQuantity(), product.getPrice());
            order.getItems().add(orderItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée ID : " + id));
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée ID : " + id));

        if (order.getStatus() != OrderStatus.CANCELLED) {
            // Restituer les stocks
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
    }
}
