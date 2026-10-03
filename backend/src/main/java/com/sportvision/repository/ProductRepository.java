package com.sportvision.repository;

import com.sportvision.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByFeaturedTrue();
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByBrandId(Long brandId);
    List<Product> findBySupplierId(Long supplierId);
    List<Product> findBySportGoalIgnoreCase(String sportGoal);

    @Query("SELECT p FROM Product p WHERE " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
           "(:sportGoal IS NULL OR LOWER(p.sportGoal) LIKE LOWER(CONCAT('%', :sportGoal, '%')))")
    List<Product> searchProducts(@Param("query") String query,
                                @Param("categoryId") Long categoryId,
                                @Param("brandId") Long brandId,
                                @Param("sportGoal") String sportGoal);

    @Query("SELECT p FROM Product p WHERE p.stock <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    long countByStockLessThanEqual(Integer threshold);
}
