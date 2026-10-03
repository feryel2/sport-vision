package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.entity.Brand;
import com.sportvision.service.BrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Brand>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.ok(brandService.getAllBrands()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> getBrandById(@PathVariable Long id) {
        return brandService.getBrandById(id)
                .map(brand -> ResponseEntity.ok(ApiResponse.ok(brand)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestBody Brand brand) {
        try {
            Brand created = brandService.createBrand(brand);
            return ResponseEntity.ok(ApiResponse.ok("Marque créée avec succès", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Brand>> updateBrand(@PathVariable Long id, @RequestBody Brand brand) {
        try {
            Brand updated = brandService.updateBrand(id, brand);
            return ResponseEntity.ok(ApiResponse.ok("Marque mise à jour avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok(ApiResponse.ok("Marque supprimée avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
