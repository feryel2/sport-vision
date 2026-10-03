package com.sportvision.repository;

import com.sportvision.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByCategoryIgnoreCase(String category);

    @Query("SELECT f FROM Food f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(f.category) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(f.benefits) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Food> searchFoods(@Param("query") String query);
}
