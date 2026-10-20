package com.sportvision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvision.entity.Category;
import com.sportvision.entity.Product;
import com.sportvision.entity.Role;
import com.sportvision.entity.User;
import com.sportvision.repository.CategoryRepository;
import com.sportvision.repository.OrderRepository;
import com.sportvision.repository.ProductRepository;
import com.sportvision.repository.UserRepository;
import com.sportvision.security.JwtService;
import com.sportvision.training.service.GeminiClient;
import com.sportvision.training.service.TrainingAiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SportVisionApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:product_order_api_tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "training.dataset.enabled=false",
        "training.demo.enabled=false",
        "sportvision.demo.enabled=false",
        "training.ai.api-key="
})
@ActiveProfiles({"test", "local-demo"})
@AutoConfigureMockMvc
class ProductOrderApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @MockBean GeminiClient gemini;

    private Category category;
    private Product product;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        category = categories.save(new Category("Test " + suffix, "test-" + suffix, null, null, null));
        product = products.save(new Product("Test product", "test-product-" + suffix, "Test", new BigDecimal("10.00"), 5,
                category, null, null, null, false, 4.5, 0, null, "Fitness"));

        User admin = users.save(new User("admin-" + suffix + "@example.invalid", "unused", "Test Admin", null, Role.ROLE_ADMIN));
        User user = users.save(new User("user-" + suffix + "@example.invalid", "unused", "Test User", null, Role.ROLE_USER));
        adminToken = "Bearer " + jwt.generateToken(admin.getEmail(), admin.getRole().name());
        userToken = "Bearer " + jwt.generateToken(user.getEmail(), user.getRole().name());
    }

    @Test
    void generatesProductRecommendationsWithGeminiAndRequiresAuthentication() throws Exception {
        when(gemini.generate(anyString(), anyString(), any())).thenReturn("""
                {"recommendations":[{"productId":%d,"matchPercentage":87,
                "recommendationReason":"Adapté à l'objectif sportif.","badge":"Sélection"}]}
                """.formatted(product.getId()));

        mvc.perform(get("/api/ai/recommendations").param("goal", "Fitness"))
                .andExpect(status().isUnauthorized());

        String response = mvc.perform(get("/api/ai/recommendations").header("Authorization", userToken)
                        .param("goal", "Fitness"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(mapper.readTree(response).path("data").get(0).path("product").path("id").asLong())
                .isEqualTo(product.getId());
        assertThat(mapper.readTree(response).path("data").get(0).path("matchPercentage").asInt()).isEqualTo(87);

        when(gemini.generate(anyString(), anyString(), any())).thenThrow(
                new TrainingAiException(503, "NOT_CONFIGURED", "Service IA non configuré"));
        mvc.perform(get("/api/ai/recommendations").header("Authorization", userToken).param("goal", "Fitness"))
                .andExpect(status().isServiceUnavailable());
    }

    @AfterEach
    void cleanUp() {
        orders.deleteAll();
        products.deleteAll(products.findByCategoryId(category.getId()));
        categories.deleteById(category.getId());
    }

    @Test
    void createsAndUpdatesValidProductsAndRejectsInvalidProductData() throws Exception {
        String createBody = productBody("Created product", "25.50", 8);
        String response = mvc.perform(post("/api/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long createdId = mapper.readTree(response).path("data").path("id").asLong();
        Product created = products.findById(createdId).orElseThrow();
        assertThat(created.getName()).isEqualTo("Created product");
        assertThat(created.getPrice()).isEqualByComparingTo("25.50");
        assertThat(created.getStock()).isEqualTo(8);

        mvc.perform(put("/api/products/" + createdId).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(productBody("Updated product", "31.25", 12)))
                .andExpect(status().isOk());
        Product updated = products.findById(createdId).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated product");
        assertThat(updated.getPrice()).isEqualByComparingTo("31.25");
        assertThat(updated.getStock()).isEqualTo(12);

        mvc.perform(post("/api/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":0,\"stock\":-1,\"categoryId\":" + category.getId() + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/products/" + createdId).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(productBody("Invalid update", "-1", -1)))
                .andExpect(status().isBadRequest());
        assertThat(products.findById(createdId).orElseThrow().getName()).isEqualTo("Updated product");
        products.deleteById(createdId);
    }

    @Test
    void rejectsNegativeStockPatchAndClampsStockAdjustmentsAtZero() throws Exception {
        mvc.perform(patch("/api/products/" + product.getId() + "/stock")
                        .header("Authorization", adminToken).param("stock", "-1"))
                .andExpect(status().isBadRequest());
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);

        mvc.perform(post("/api/products/" + product.getId() + "/stock/adjust")
                        .header("Authorization", adminToken).param("delta", "-2"))
                .andExpect(status().isOk());
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);

        mvc.perform(post("/api/products/" + product.getId() + "/stock/adjust")
                        .header("Authorization", adminToken).param("delta", "-10"))
                .andExpect(status().isOk());
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();

        mvc.perform(post("/api/products/" + product.getId() + "/stock/adjust")
                        .header("Authorization", adminToken).param("delta", "4"))
                .andExpect(status().isOk());
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(4);
    }

    @Test
    void rejectsZeroAndNegativePricesAndAcceptsPositivePrice() throws Exception {
        mvc.perform(patch("/api/products/" + product.getId() + "/price")
                        .header("Authorization", adminToken).param("price", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/products/" + product.getId() + "/price")
                        .header("Authorization", adminToken).param("price", "-2.50"))
                .andExpect(status().isBadRequest());
        assertThat(products.findById(product.getId()).orElseThrow().getPrice()).isEqualByComparingTo("10.00");

        mvc.perform(patch("/api/products/" + product.getId() + "/price")
                        .header("Authorization", adminToken).param("price", "12.75"))
                .andExpect(status().isOk());
        assertThat(products.findById(product.getId()).orElseThrow().getPrice()).isEqualByComparingTo("12.75");
    }

    @Test
    void handlesMissingProductsAndProtectsProductAdministration() throws Exception {
        mvc.perform(get("/api/products/999999999")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products/featured")).andExpect(status().isOk());
        mvc.perform(get("/api/products").header("Authorization", userToken)).andExpect(status().isOk());
        mvc.perform(post("/api/products").header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(productBody("Forbidden", "5", 1)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/products/999999999").header("Authorization", userToken))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/products/999999999/stock").header("Authorization", adminToken).param("stock", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsAnOrderWithValidQuantityAndUpdatesStockAndTotal() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody("2")))
                .andExpect(status().isOk());

        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
        assertThat(orders.findAll()).hasSize(1);
        assertThat(orders.findAll().get(0).getTotalAmount()).isEqualByComparingTo("20.00");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsZeroAndNegativeOrderQuantitiesWithoutChangingStockOrCreatingOrder(int quantity) throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(Integer.toString(quantity))))
                .andExpect(status().isBadRequest());

        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(orders.findAll()).isEmpty();
    }

    @Test
    void rejectsMissingOrderQuantityWithoutChangingStockOrCreatingOrder() throws Exception {
        String body = orderBody("1").replace("\"quantity\":1", "\"quantity\":null");
        mvc.perform(post("/api/orders").header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(orders.findAll()).isEmpty();
    }

    private String productBody(String name, String price, int stock) {
        return "{\"name\":\"" + name + "\",\"description\":\"Test\",\"price\":" + price
                + ",\"stock\":" + stock + ",\"categoryId\":" + category.getId() + "}";
    }

    private String orderBody(String quantity) {
        return "{\"customerName\":\"Test Customer\",\"customerEmail\":\"customer@example.invalid\","
                + "\"shippingAddress\":\"Test address\",\"items\":[{\"productId\":" + product.getId()
                + ",\"quantity\":" + quantity + "}]}";
    }
}
