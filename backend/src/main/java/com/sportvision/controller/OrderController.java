package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.dto.OrderRequest;
import com.sportvision.entity.Order;
import com.sportvision.entity.OrderStatus;
import com.sportvision.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(order -> ResponseEntity.ok(ApiResponse.ok(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Order>>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUser(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody OrderRequest request) {
        try {
            Order created = orderService.createOrder(request);
            return ResponseEntity.ok(ApiResponse.ok("Commande enregistrée avec succès !", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        try {
            Order updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.ok("Statut de commande mis à jour", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrder(id);
            return ResponseEntity.ok(ApiResponse.ok("Commande annulée et stocks restitués", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
