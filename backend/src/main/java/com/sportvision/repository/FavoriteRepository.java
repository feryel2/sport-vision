package com.sportvision.repository;

import com.sportvision.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    Optional<Favorite> findByUserEmailAndProductId(String userEmail, Long productId);
    boolean existsByUserEmailAndProductId(String userEmail, Long productId);
    void deleteByUserEmailAndProductId(String userEmail, Long productId);
}
