package com.meethybridhub.catalog;

import com.meethybridhub.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock InventoryRepository inventoryRepository;

    @Test
    void createVariantCreatesInitialInventory() {
        Product product = product(11L, 7L);
        when(productRepository.findByIdAndStoreId(11L, 7L)).thenReturn(Optional.of(product));
        when(variantRepository.findByStoreIdAndSku(7L, "SKU-1")).thenReturn(Optional.empty());
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant variant = invocation.getArgument(0);
            ReflectionTestUtils.setField(variant, "id", 22L);
            return variant;
        });
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariant result = service().create(7L, 11L, " SKU-1 ", " 42 ", " Black ", null, 5);

        assertThat(result.getSku()).isEqualTo("SKU-1");
        verify(inventoryRepository).save(argThat(stock -> stock.getQuantity() == 5));
    }

    @Test
    void createVariantRejectsDuplicateSkuAndInventoryRejectsOverReservation() {
        Product product = product(11L, 7L);
        when(productRepository.findByIdAndStoreId(11L, 7L)).thenReturn(Optional.of(product));
        when(variantRepository.findByStoreIdAndSku(7L, "SKU-1")).thenReturn(Optional.of(variant(22L, 7L, product)));
        assertThatThrownBy(() -> service().create(7L, 11L, "SKU-1", null, null, null, 1))
                .isInstanceOf(BadRequestException.class);

        ProductVariant variant = variant(22L, 7L, product);
        when(variantRepository.findByIdAndStoreId(22L, 7L)).thenReturn(Optional.of(variant));
        assertThatThrownBy(() -> service().updateInventory(7L, 22L, 1, 2))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listAndInventoryOperationsStayWithinTenant() {
        Product product = product(11L, 7L);
        ProductVariant variant = variant(22L, 7L, product);
        Inventory inventory = new Inventory(7L, variant, 10);
        when(productRepository.existsByIdAndStoreId(11L, 7L)).thenReturn(true);
        when(variantRepository.findAllByStoreIdAndProductId(7L, 11L)).thenReturn(List.of(variant));
        when(variantRepository.findByIdAndStoreId(22L, 7L)).thenReturn(Optional.of(variant));
        when(inventoryRepository.findByStoreIdAndVariantId(7L, 22L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductVariantService service = service();

        assertThat(service.list(7L, 11L)).containsExactly(variant);
        Inventory updated = service.updateInventory(7L, 22L, 12, 3);
        assertThat(updated.getAvailableQuantity()).isEqualTo(9);
        assertThat(service.getInventory(7L, 22L)).isSameAs(inventory);
    }

    private ProductVariantService service() {
        return new ProductVariantService(productRepository, variantRepository, inventoryRepository);
    }

    private Product product(Long id, Long storeId) {
        Product product = new Product(storeId, "Shoe", null, new BigDecimal("5000.00"), null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductVariant variant(Long id, Long storeId, Product product) {
        ProductVariant variant = new ProductVariant(storeId, product, "SKU-1", "42", "Black", null);
        ReflectionTestUtils.setField(variant, "id", id);
        return variant;
    }
}
