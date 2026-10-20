package com.sportvision.service;

import com.sportvision.dto.ProductDto;
import com.sportvision.entity.Category;
import com.sportvision.entity.Product;
import com.sportvision.repository.BrandRepository;
import com.sportvision.repository.CategoryRepository;
import com.sportvision.repository.ProductRepository;
import com.sportvision.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock BrandRepository brands;
    @Mock SupplierRepository suppliers;

    private ProductService service;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, categories, brands, suppliers);
        category = new Category("Fitness", "fitness", null, null, null);
        category.setId(7L);
    }

    @Test
    void createsAProductWithNormalizedSlugAndResolvedCategory() {
        ProductDto dto = productDto("Resistance Bands", "25.50", 12);
        dto.setSlug("Resistance Bands Set");
        when(categories.findById(7L)).thenReturn(Optional.of(category));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = service.createProduct(dto);

        assertThat(created.getName()).isEqualTo("Resistance Bands");
        assertThat(created.getSlug()).isEqualTo("resistance-bands-set");
        assertThat(created.getPrice()).isEqualByComparingTo("25.50");
        assertThat(created.getStock()).isEqualTo(12);
        assertThat(created.getCategory()).isSameAs(category);
        verify(products).save(created);
    }

    @Test
    void updatesAnExistingProductAndPersistsTheNewValues() {
        Product existing = new Product("Old name", "old-name", "Old description", new BigDecimal("10.00"), 2,
                category, null, null, null, false, 4.0, 0, null, "Fitness");
        ProductDto dto = productDto("Updated name", "19.99", 6);
        dto.setSlug("Updated Name");
        when(products.findById(3L)).thenReturn(Optional.of(existing));
        when(categories.findById(7L)).thenReturn(Optional.of(category));
        when(products.save(existing)).thenReturn(existing);

        Product updated = service.updateProduct(3L, dto);

        assertThat(updated.getName()).isEqualTo("Updated name");
        assertThat(updated.getSlug()).isEqualTo("updated-name");
        assertThat(updated.getPrice()).isEqualByComparingTo("19.99");
        assertThat(updated.getStock()).isEqualTo(6);
        verify(products).save(existing);
    }

    @Test
    void rejectsNegativeStockWithoutLookingUpOrSavingAProduct() {
        assertThatThrownBy(() -> service.updateStock(3L, -1))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(products);
    }

    @Test
    void rejectsNullStockWithoutLookingUpOrSavingAProduct() {
        assertThatThrownBy(() -> service.updateStock(3L, null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(products);
    }

    @Test
    void updateStockSavesAValidValue() {
        Product existing = productWithStock(4);
        when(products.findById(3L)).thenReturn(Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        Product updated = service.updateStock(3L, 0);

        assertThat(updated.getStock()).isZero();
        verify(products).save(existing);
    }

    @Test
    void stockAdjustmentsClampAtZeroAndApplyPositiveDeltas() {
        Product existing = productWithStock(2);
        when(products.findById(3L)).thenReturn(Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        assertThat(service.adjustStock(3L, -5).getStock()).isZero();
        assertThat(service.adjustStock(3L, 4).getStock()).isEqualTo(4);
        verify(products, times(2)).save(existing);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01", "-10"})
    void rejectsNullZeroAndNegativePrices(BigDecimal price) {
        assertThatThrownBy(() -> service.updatePrice(3L, price))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(products);
    }

    @Test
    void savesAPositivePrice() {
        Product existing = productWithStock(2);
        when(products.findById(3L)).thenReturn(Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        Product updated = service.updatePrice(3L, new BigDecimal("14.25"));

        assertThat(updated.getPrice()).isEqualByComparingTo("14.25");
        verify(products).save(existing);
    }

    @Test
    void reportsWhenAProductDoesNotExistForStockAndPriceChanges() {
        when(products.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStock(404L, 4))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produit non trouvé");
        assertThatThrownBy(() -> service.updatePrice(404L, new BigDecimal("4.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produit non trouvé");
        verify(products, never()).save(any(Product.class));
    }

    private Product productWithStock(int stock) {
        return new Product("Test", "test", null, new BigDecimal("10.00"), stock,
                category, null, null, null, false, 4.0, 0, null, "Fitness");
    }

    private ProductDto productDto(String name, String price, int stock) {
        ProductDto dto = new ProductDto();
        dto.setName(name);
        dto.setDescription("Unit test product");
        dto.setPrice(new BigDecimal(price));
        dto.setStock(stock);
        dto.setCategoryId(7L);
        dto.setSportGoal("Fitness");
        return dto;
    }
}
