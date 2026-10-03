package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.entity.Favorite;
import com.sportvision.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Favorite>>> getFavorites(
            @RequestParam(defaultValue = "user@sportvision.com") String userEmail) {
        return ResponseEntity.ok(ApiResponse.ok(favoriteService.getFavorites(userEmail)));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(
            @RequestParam(defaultValue = "user@sportvision.com") String userEmail,
            @RequestParam Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(favoriteService.isFavorite(userEmail, productId)));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(
            @RequestParam(defaultValue = "user@sportvision.com") String userEmail,
            @RequestParam Long productId) {
        try {
            boolean favorited = favoriteService.toggleFavorite(userEmail, productId);
            String msg = favorited ? "Ajouté aux favoris" : "Retiré des favoris";
            return ResponseEntity.ok(ApiResponse.ok(msg, Map.of("favorited", favorited, "productId", productId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Favorite>> addFavorite(
            @RequestParam(defaultValue = "user@sportvision.com") String userEmail,
            @RequestParam Long productId) {
        try {
            Favorite favorite = favoriteService.addFavorite(userEmail, productId);
            return ResponseEntity.ok(ApiResponse.ok("Ajouté aux favoris", favorite));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestParam(defaultValue = "user@sportvision.com") String userEmail,
            @RequestParam Long productId) {
        try {
            favoriteService.removeFavorite(userEmail, productId);
            return ResponseEntity.ok(ApiResponse.ok("Retiré des favoris", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
