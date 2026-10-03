package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.entity.Food;
import com.sportvision.service.FoodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Food>>> getAllFoods(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        List<Food> foods;
        if (category != null && !category.isBlank()) {
            foods = foodService.getFoodsByCategory(category);
        } else if (query != null && !query.isBlank()) {
            foods = foodService.searchFoods(query);
        } else {
            foods = foodService.getAllFoods();
        }
        return ResponseEntity.ok(ApiResponse.ok(foods));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Food>> getFoodById(@PathVariable Long id) {
        return foodService.getFoodById(id)
                .map(food -> ResponseEntity.ok(ApiResponse.ok(food)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Food>> createFood(@RequestBody Food food) {
        try {
            Food created = foodService.createFood(food);
            return ResponseEntity.ok(ApiResponse.ok("Aliment ajouté avec succès", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Food>> updateFood(@PathVariable Long id, @RequestBody Food food) {
        try {
            Food updated = foodService.updateFood(id, food);
            return ResponseEntity.ok(ApiResponse.ok("Aliment mis à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {
        try {
            foodService.deleteFood(id);
            return ResponseEntity.ok(ApiResponse.ok("Aliment supprimé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
