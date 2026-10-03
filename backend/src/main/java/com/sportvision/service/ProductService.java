package com.sportvision.service;

import com.sportvision.dto.ProductDto;
import com.sportvision.entity.Brand;
import com.sportvision.entity.Category;
import com.sportvision.entity.Product;
import com.sportvision.entity.Supplier;
import com.sportvision.repository.BrandRepository;
import com.sportvision.repository.CategoryRepository;
import com.sportvision.repository.ProductRepository;
import com.sportvision.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SupplierRepository supplierRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          BrandRepository brandRepository,
                          SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public List<Product> getFeaturedProducts() {
        return productRepository.findByFeaturedTrue();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByBrand(Long brandId) {
        return productRepository.findByBrandId(brandId);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsBySportGoal(String sportGoal) {
        return productRepository.findBySportGoalIgnoreCase(sportGoal);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String query, Long categoryId, Long brandId, String sportGoal) {
        return productRepository.searchProducts(
                (query != null && !query.isBlank()) ? query : null,
                categoryId,
                brandId,
                (sportGoal != null && !sportGoal.isBlank()) ? sportGoal : null
        );
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findLowStockProducts(threshold != null ? threshold : 5);
    }

    public Product createProduct(ProductDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id : " + dto.getCategoryId()));

        Brand brand = null;
        if (dto.getBrandId() != null) {
            brand = brandRepository.findById(dto.getBrandId()).orElse(null);
        }

        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId()).orElse(null);
        }

        String slug = (dto.getSlug() != null && !dto.getSlug().isBlank()) ?
                toSlug(dto.getSlug()) : toSlug(dto.getName());

        Product product = new Product(
                dto.getName(),
                slug,
                dto.getDescription(),
                dto.getPrice(),
                dto.getStock(),
                category,
                brand,
                supplier,
                dto.getImageUrl(),
                dto.getFeatured() != null ? dto.getFeatured() : false,
                dto.getRating() != null ? dto.getRating() : 4.8,
                dto.getReviewsCount() != null ? dto.getReviewsCount() : 10,
                dto.getSpecifications(),
                dto.getSportGoal()
        );

        return productRepository.save(product);
    }

    public Product updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + id));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id : " + dto.getCategoryId()));

        Brand brand = null;
        if (dto.getBrandId() != null) {
            brand = brandRepository.findById(dto.getBrandId()).orElse(null);
        }

        Supplier supplier = null;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId()).orElse(null);
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);
        product.setBrand(brand);
        product.setSupplier(supplier);
        product.setSportGoal(dto.getSportGoal());
        product.setImageUrl(dto.getImageUrl());
        if (dto.getFeatured() != null) product.setFeatured(dto.getFeatured());
        if (dto.getRating() != null) product.setRating(dto.getRating());
        if (dto.getReviewsCount() != null) product.setReviewsCount(dto.getReviewsCount());
        if (dto.getSpecifications() != null) product.setSpecifications(dto.getSpecifications());

        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            product.setSlug(toSlug(dto.getSlug()));
        }

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public Product updateStock(Long id, Integer newStock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + id));
        product.setStock(newStock);
        return productRepository.save(product);
    }

    public Product adjustStock(Long id, Integer delta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + id));
        int updated = product.getStock() + delta;
        if (updated < 0) updated = 0;
        product.setStock(updated);
        return productRepository.save(product);
    }

    public Product updatePrice(Long id, BigDecimal newPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + id));
        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être strictement supérieur à zéro");
        }
        product.setPrice(newPrice);
        return productRepository.save(product);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
