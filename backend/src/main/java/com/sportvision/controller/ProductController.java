package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.dto.ProductDto;
import com.sportvision.entity.Product;
import com.sportvision.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String sportGoal) {

        List<Product> products;
        if ((query != null && !query.isBlank()) || categoryId != null || brandId != null || (sportGoal != null && !sportGoal.isBlank())) {
            products = productService.searchProducts(query, categoryId, brandId, sportGoal);
        } else {
            products = productService.getAllProducts();
        }
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<Product>>> getFeaturedProducts() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getFeaturedProducts()));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStockProducts(
            @RequestParam(defaultValue = "5") Integer threshold) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getLowStockProducts(threshold)));
    }

    @GetMapping("/goal/{sportGoal}")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsBySportGoal(@PathVariable String sportGoal) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductsBySportGoal(sportGoal)));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductsByBrand(brandId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(prod -> ResponseEntity.ok(ApiResponse.ok(prod)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody ProductDto dto) {
        try {
            Product created = productService.createProduct(dto);
            return ResponseEntity.ok(ApiResponse.ok("Produit ajouté avec succès", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDto dto) {
        try {
            Product updated = productService.updateProduct(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Produit mis à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Product>> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        try {
            Product updated = productService.updateStock(id, stock);
            return ResponseEntity.ok(ApiResponse.ok("Stock mis à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/stock/adjust")
    public ResponseEntity<ApiResponse<Product>> adjustStock(@PathVariable Long id, @RequestParam Integer delta) {
        try {
            Product updated = productService.adjustStock(id, delta);
            return ResponseEntity.ok(ApiResponse.ok("Ajustement de stock effectué", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ApiResponse<Product>> updatePrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        try {
            Product updated = productService.updatePrice(id, price);
            return ResponseEntity.ok(ApiResponse.ok("Prix mis à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.ok("Produit supprimé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
