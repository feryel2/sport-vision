package com.sportvision.service;

import com.sportvision.dto.DashboardStatsDto;
import com.sportvision.entity.Category;
import com.sportvision.entity.OrderStatus;
import com.sportvision.entity.Role;
import com.sportvision.repository.CategoryRepository;
import com.sportvision.repository.OrderRepository;
import com.sportvision.repository.ProductRepository;
import com.sportvision.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public StatsService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        CategoryRepository categoryRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto dto = new DashboardStatsDto();

        dto.setTotalRevenue(orderRepository.sumTotalRevenue());
        dto.setTotalOrders(orderRepository.count());
        dto.setTotalProducts(productRepository.count());
        dto.setTotalCustomers(userRepository.countByRole(Role.ROLE_USER));
        dto.setLowStockCount(productRepository.countByStockLessThanEqual(5));
        dto.setPendingOrdersCount(orderRepository.countByStatus(OrderStatus.PENDING) + orderRepository.countByStatus(OrderStatus.CONFIRMED));

        // Produits par catégorie
        Map<String, Long> categoryDistribution = new LinkedHashMap<>();
        List<Category> categories = categoryRepository.findAll();
        for (Category cat : categories) {
            long count = productRepository.findByCategoryId(cat.getId()).size();
            categoryDistribution.put(cat.getName(), count);
        }
        dto.setProductsByCategory(categoryDistribution);

        // Simulation de ventes mensuelles pour le dashboard backoffice
        Map<String, BigDecimal> monthlySales = new LinkedHashMap<>();
        monthlySales.put("Jan", new BigDecimal("12450.00"));
        monthlySales.put("Fév", new BigDecimal("15800.00"));
        monthlySales.put("Mar", new BigDecimal("18920.00"));
        monthlySales.put("Avr", new BigDecimal("22400.00"));
        monthlySales.put("Mai", new BigDecimal("26100.00"));
        monthlySales.put("Juin", new BigDecimal("31500.00"));
        monthlySales.put("Juil", new BigDecimal("38900.00"));
        monthlySales.put("Août", new BigDecimal("34200.00"));
        monthlySales.put("Sept", new BigDecimal("29800.00"));
        monthlySales.put("Oct", new BigDecimal("33500.00"));
        dto.setMonthlySales(monthlySales);

        return dto;
    }
}
