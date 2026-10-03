package com.sportvision.service;

import com.sportvision.entity.Favorite;
import com.sportvision.entity.Product;
import com.sportvision.repository.FavoriteRepository;
import com.sportvision.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavorites(String userEmail) {
        return favoriteRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String userEmail, Long productId) {
        return favoriteRepository.existsByUserEmailAndProductId(userEmail, productId);
    }

    public Favorite addFavorite(String userEmail, Long productId) {
        Optional<Favorite> existing = favoriteRepository.findByUserEmailAndProductId(userEmail, productId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + productId));

        Favorite favorite = new Favorite(userEmail, product);
        return favoriteRepository.save(favorite);
    }

    public void removeFavorite(String userEmail, Long productId) {
        favoriteRepository.deleteByUserEmailAndProductId(userEmail, productId);
    }

    public boolean toggleFavorite(String userEmail, Long productId) {
        Optional<Favorite> existing = favoriteRepository.findByUserEmailAndProductId(userEmail, productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + productId));
            favoriteRepository.save(new Favorite(userEmail, product));
            return true;
        }
    }
}
