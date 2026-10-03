package com.sportvision.service;

import com.sportvision.dto.ReviewDto;
import com.sportvision.entity.Product;
import com.sportvision.entity.Review;
import com.sportvision.repository.ProductRepository;
import com.sportvision.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review addReview(ReviewDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id : " + dto.getProductId()));

        Review review = new Review(
                product,
                dto.getAuthorName(),
                dto.getAuthorEmail(),
                dto.getRating(),
                dto.getComment()
        );

        Review saved = reviewRepository.save(review);
        updateProductRatingStats(product.getId());
        return saved;
    }

    public Review updateReview(Long id, ReviewDto dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'id : " + id));

        review.setAuthorName(dto.getAuthorName());
        if (dto.getAuthorEmail() != null) review.setAuthorEmail(dto.getAuthorEmail());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review updated = reviewRepository.save(review);
        updateProductRatingStats(review.getProduct().getId());
        return updated;
    }

    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'id : " + id));
        Long productId = review.getProduct().getId();
        reviewRepository.deleteById(id);
        updateProductRatingStats(productId);
    }

    private void updateProductRatingStats(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Double avg = reviewRepository.calculateAverageRatingForProduct(productId);
            Long count = reviewRepository.countByProductId(productId);
            if (avg != null) {
                // Round to 1 decimal place
                product.setRating(Math.round(avg * 10.0) / 10.0);
            }
            if (count != null) {
                product.setReviewsCount(count.intValue());
            }
            productRepository.save(product);
        }
    }
}
