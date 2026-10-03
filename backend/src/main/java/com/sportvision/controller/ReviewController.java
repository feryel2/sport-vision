package com.sportvision.controller;

import com.sportvision.dto.ApiResponse;
import com.sportvision.dto.ReviewDto;
import com.sportvision.entity.Review;
import com.sportvision.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Review>>> getAllReviews() {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getAllReviews()));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviewsByProduct(productId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(rev -> ResponseEntity.ok(ApiResponse.ok(rev)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Review>> addReview(@Valid @RequestBody ReviewDto dto) {
        try {
            Review created = reviewService.addReview(dto);
            return ResponseEntity.ok(ApiResponse.ok("Avis ajouté avec succès", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> updateReview(@PathVariable Long id, @Valid @RequestBody ReviewDto dto) {
        try {
            Review updated = reviewService.updateReview(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Avis modifié avec succès", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok(ApiResponse.ok("Avis supprimé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
