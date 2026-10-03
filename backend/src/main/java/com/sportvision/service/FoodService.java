package com.sportvision.service;

import com.sportvision.entity.Food;
import com.sportvision.repository.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FoodService {

    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    @Transactional(readOnly = true)
    public List<Food> getAllFoods() {
        return foodRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Food> getFoodById(Long id) {
        return foodRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Food> getFoodsByCategory(String category) {
        return foodRepository.findByCategoryIgnoreCase(category);
    }

    @Transactional(readOnly = true)
    public List<Food> searchFoods(String query) {
        if (query == null || query.isBlank()) {
            return foodRepository.findAll();
        }
        return foodRepository.searchFoods(query);
    }

    public Food createFood(Food food) {
        return foodRepository.save(food);
    }

    public Food updateFood(Long id, Food details) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aliment non trouvé avec l'id : " + id));

        food.setName(details.getName());
        food.setCategory(details.getCategory());
        food.setCalories(details.getCalories());
        food.setProteins(details.getProteins());
        food.setCarbs(details.getCarbs());
        food.setFats(details.getFats());
        food.setPortion(details.getPortion());
        food.setBenefits(details.getBenefits());

        return foodRepository.save(food);
    }

    public void deleteFood(Long id) {
        foodRepository.deleteById(id);
    }
}
