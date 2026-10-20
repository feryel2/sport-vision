package com.sportvision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvision.entity.Category;
import com.sportvision.entity.Product;
import com.sportvision.training.service.GeminiClient;
import com.sportvision.training.service.TrainingAiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductRecommendationAiServiceTest {
    @Mock GeminiClient gemini;

    private final ObjectMapper mapper = new ObjectMapper();
    private ProductRecommendationAiService service;
    private Product bands;
    private Product shoes;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProductRecommendationAiService(gemini, mapper);
        Category category = new Category("Musculation", "musculation", "", "", "");
        bands = product(1L, "Bandes elastiques", "Resistance progressive", "Musculation", category);
        shoes = product(2L, "Chaussures running", "Amorti", "Course", category);
    }

    @Test
    void mapsStructuredGeminiRecommendationsToExistingProductDtos() {
        when(gemini.generate(anyString(), anyString(), any(JsonNode.class))).thenReturn("""
                {"recommendations":[
                  {"productId":2,"matchPercentage":91,"recommendationReason":"Adaptees a la course.","badge":"Course"},
                  {"productId":1,"matchPercentage":80,"recommendationReason":"Resistance pour la musculation.","badge":"Force"}
                ]}
                """);

        var results = service.recommend("Course", List.of(bands, shoes), Set.of(1L), Set.of(2L));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getProduct()).isSameAs(shoes);
        assertThat(results.get(0).getMatchPercentage()).isEqualTo(91);
        assertThat(results.get(1).getProduct()).isSameAs(bands);
        ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> schema = ArgumentCaptor.forClass(JsonNode.class);
        verify(gemini).generate(contains("estimation comparative"), data.capture(), schema.capture());
        assertThat(data.getValue()).contains("\"goal\":\"Course\"", "\"favorite\":true", "\"previouslyOrdered\":true")
                .doesNotContain("userEmail");
        assertThat(schema.getValue().path("properties").has("recommendations")).isTrue();
    }

    @Test
    void rejectsUnknownProductIdsAndOutOfRangeScoresFromModel() {
        when(gemini.generate(anyString(), anyString(), any(JsonNode.class))).thenReturn(
                "{\"recommendations\":[{\"productId\":999,\"matchPercentage\":90,\"recommendationReason\":\"Raison\",\"badge\":\"Test\"}]}");
        assertThatThrownBy(() -> service.recommend("Course", List.of(bands), Set.of(), Set.of()))
                .isInstanceOf(TrainingAiException.class)
                .hasMessageContaining("réponse IA de recommandations est invalide");

        when(gemini.generate(anyString(), anyString(), any(JsonNode.class))).thenReturn(
                "{\"recommendations\":[{\"productId\":1,\"matchPercentage\":101,\"recommendationReason\":\"Raison\",\"badge\":\"Test\"}]}");
        assertThatThrownBy(() -> service.recommend("Course", List.of(bands), Set.of(), Set.of()))
                .isInstanceOf(TrainingAiException.class)
                .hasMessageContaining("réponse IA de recommandations est invalide");
    }

    @Test
    void rejectsDuplicateProductsAndMalformedJson() {
        when(gemini.generate(anyString(), anyString(), any(JsonNode.class))).thenReturn("""
                {"recommendations":[
                  {"productId":1,"matchPercentage":90,"recommendationReason":"Une raison.","badge":"Test"},
                  {"productId":1,"matchPercentage":85,"recommendationReason":"Une autre raison.","badge":"Test"}
                ]}
                """);
        assertThatThrownBy(() -> service.recommend("Course", List.of(bands), Set.of(), Set.of()))
                .isInstanceOf(TrainingAiException.class);

        when(gemini.generate(anyString(), anyString(), any(JsonNode.class))).thenReturn("not-json");
        assertThatThrownBy(() -> service.recommend("Course", List.of(bands), Set.of(), Set.of()))
                .isInstanceOf(TrainingAiException.class);
    }

    private Product product(Long id, String name, String description, String sportGoal, Category category) {
        Product product = new Product(name, name.toLowerCase().replace(' ', '-'), description,
                BigDecimal.TEN, 5, category, null, null, null, false, 4.5, 0, null, sportGoal);
        product.setId(id);
        return product;
    }
}
