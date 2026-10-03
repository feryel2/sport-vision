package com.sportvision.service;

import com.sportvision.entity.Category;
import com.sportvision.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(toSlug(category.getName()));
        }
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id : " + id));

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setIcon(categoryDetails.getIcon());
        category.setImageUrl(categoryDetails.getImageUrl());
        if (categoryDetails.getSlug() != null && !categoryDetails.getSlug().isBlank()) {
            category.setSlug(toSlug(categoryDetails.getSlug()));
        }

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
