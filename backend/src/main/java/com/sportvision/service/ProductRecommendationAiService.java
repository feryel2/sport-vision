package com.sportvision.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.sportvision.dto.ProductRecommendationDto;
import com.sportvision.entity.Product;
import com.sportvision.training.service.GeminiClient;
import com.sportvision.training.service.TrainingAiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ProductRecommendationAiService {
    private static final int MAX_RECOMMENDATIONS = 8;
    private static final int MAX_REASON_LENGTH = 300;
    private static final int MAX_BADGE_LENGTH = 80;

    private final GeminiClient gemini;
    private final ObjectMapper mapper;
    private final ObjectReader responseReader;
    private final String prompt;
    private final JsonNode schema;

    public ProductRecommendationAiService(GeminiClient gemini, ObjectMapper mapper) throws IOException {
        this.gemini = gemini;
        this.mapper = mapper;
        this.prompt = new ClassPathResource("products/ai/recommendation-prompt.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        this.schema = mapper.readTree(new ClassPathResource("products/ai/recommendation-schema.json")
                .getContentAsString(StandardCharsets.UTF_8));
        this.responseReader = mapper.copy()
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .readerFor(RecommendationResponse.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    public List<ProductRecommendationDto> recommend(String goal, List<Product> products,
            Set<Long> favoriteProductIds, Set<Long> orderedProductIds) {
        gemini.requireConfigured();
        String normalizedGoal = goal == null || goal.isBlank() ? "musculation" : goal.trim();
        Map<Long, Product> productsById = new HashMap<>();
        List<CatalogProduct> catalogue = products.stream().map(product -> {
            productsById.put(product.getId(), product);
            return new CatalogProduct(
                    product.getId(), product.getName(), product.getDescription(), product.getSportGoal(),
                    product.getCategory() == null ? null : product.getCategory().getName(),
                    Boolean.TRUE.equals(product.getFeatured()), product.getRating(),
                    favoriteProductIds.contains(product.getId()), orderedProductIds.contains(product.getId()));
        }).toList();

        String response;
        try {
            String data = mapper.writeValueAsString(Map.of("goal", normalizedGoal, "catalogue", catalogue));
            response = gemini.generate(prompt, data, schema);
        } catch (TrainingAiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable();
        }

        RecommendationResponse parsed;
        try {
            parsed = responseReader.readValue(response);
        } catch (Exception ex) {
            throw invalidResponse();
        }
        if (parsed == null || parsed.recommendations() == null || parsed.recommendations().isEmpty()
                || parsed.recommendations().size() > MAX_RECOMMENDATIONS) {
            throw invalidResponse();
        }

        Set<Long> selectedIds = new HashSet<>();
        for (RecommendationItem item : parsed.recommendations()) {
            if (item == null || item.productId() == null || !productsById.containsKey(item.productId())
                    || !selectedIds.add(item.productId())
                    || item.matchPercentage() == null || item.matchPercentage() < 0 || item.matchPercentage() > 100
                    || !validText(item.recommendationReason(), MAX_REASON_LENGTH)
                    || !validText(item.badge(), MAX_BADGE_LENGTH)) {
                throw invalidResponse();
            }
        }

        return parsed.recommendations().stream()
                .map(item -> new ProductRecommendationDto(productsById.get(item.productId()), item.matchPercentage(),
                        item.recommendationReason(), item.badge()))
                .toList();
    }

    private boolean validText(String value, int maxLength) {
        return value != null && !value.isBlank() && value.length() <= maxLength;
    }

    private TrainingAiException invalidResponse() {
        return new TrainingAiException(502, "INVALID_RECOMMENDATIONS",
                "La réponse IA de recommandations est invalide. Réessayez plus tard.");
    }

    private TrainingAiException unavailable() {
        return new TrainingAiException(503, "UNAVAILABLE", "Service de recommandations indisponible.");
    }

    private record CatalogProduct(Long id, String name, String description, String sportGoal,
            String category, boolean featured, Double rating, boolean favorite, boolean previouslyOrdered) {}

    private record RecommendationResponse(List<RecommendationItem> recommendations) {}

    private record RecommendationItem(Long productId, Integer matchPercentage,
            String recommendationReason, String badge) {}
}
