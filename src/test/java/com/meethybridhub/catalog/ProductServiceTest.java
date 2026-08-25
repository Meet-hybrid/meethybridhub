package com.meethybridhub.catalog;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;

    @Test
    void createSupportsProductsWithAndWithoutCategories() {
        Category category = category(2L, 7L);
        when(categoryRepository.findByIdAndStoreId(2L, 7L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductService service = new ProductService(productRepository, categoryRepository);

        Product result = service.create(7L, " Sneaker ", " desc ", new BigDecimal("5000.00"), 2L);
        assertThat(result.getName()).isEqualTo("Sneaker");
        assertThat(result.getStoreId()).isEqualTo(7L);
        assertThat(result.getCategory()).isSameAs(category);

        Product withoutCategory = service.create(7L, "Plain", null, BigDecimal.TEN, null);
        assertThat(withoutCategory.getCategory()).isNull();
    }

    @Test
    void createAndUpdateRejectMissingCategories() {
        when(categoryRepository.findByIdAndStoreId(99L, 7L)).thenReturn(Optional.empty());
        ProductService service = new ProductService(productRepository, categoryRepository);
        assertThatThrownBy(() -> service.create(7L, "x", null, BigDecimal.ONE, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        Product product = product(3L, 7L);
        when(productRepository.findByIdAndStoreId(3L, 7L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.update(7L, 3L, "x", null, BigDecimal.ONE, 99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listGetUpdateDeleteAndSearchUseTenantRepository() {
        Product product = product(3L, 7L);
        PageRequest page = PageRequest.of(0, 20);
        when(productRepository.findAllByStoreIdAndActiveTrue(7L, page)).thenReturn(new PageImpl<>(List.of(product)));
        when(productRepository.findAllByStoreId(7L, page)).thenReturn(new PageImpl<>(List.of(product)));
        when(productRepository.findByIdAndStoreId(3L, 7L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.search(eq(7L), eq(true), eq("shoe"), eq(2L), any(), any(), eq(page)))
                .thenReturn(new PageImpl<>(List.of(product)));
        ProductService service = new ProductService(productRepository, categoryRepository);

        assertThat(service.list(7L, page, true).getContent()).containsExactly(product);
        assertThat(service.list(7L, page, false).getContent()).containsExactly(product);
        assertThat(service.get(7L, 3L)).isSameAs(product);
        assertThat(service.update(7L, 3L, " Updated ", " d ", BigDecimal.ONE, null, false).isActive()).isFalse();
        service.delete(7L, 3L);
        verify(productRepository).delete(product);
        assertThat(service.search(7L, page, true, " shoe ", 2L, null, null).getContent()).containsExactly(product);
    }

    @Test
    void searchRejectsReversedPriceRange() {
        assertThatThrownBy(() -> new ProductService(productRepository, categoryRepository)
                .search(7L, PageRequest.of(0, 10), true, null, null, BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(BadRequestException.class);
    }

    private Category category(Long id, Long storeId) {
        Category category = new Category(storeId, "Shoes", null, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Product product(Long id, Long storeId) {
        Product product = new Product(storeId, "Shoe", "desc", new BigDecimal("5000.00"), null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
