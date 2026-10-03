package com.sportvision.repository;

import com.sportvision.entity.Order;
import com.sportvision.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findTop5ByOrderByCreatedAtDesc();
    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal sumTotalRevenue();
}
