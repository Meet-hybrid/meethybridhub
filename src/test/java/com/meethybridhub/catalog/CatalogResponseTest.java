package com.meethybridhub.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogResponseTest {

    @Test
    void categoryResponsesHandleParentAndRequestDefaults() {
        Category parent = new Category(7L, "Shoes", "Footwear", null);
        ReflectionTestUtils.setField(parent, "id", 10L);
        Category child = new Category(7L, "Trainers", null, parent);
        ReflectionTestUtils.setField(child, "id", 11L);

        CategoryController.CategoryRequest request =
                new CategoryController.CategoryRequest("Trainers", "Footwear", 10L);
        CategoryController.CategoryResponse response = CategoryController.CategoryResponse.from(child);

        assertThat(request.name()).isEqualTo("Trainers");
        assertThat(response.parentId()).isEqualTo(10L);
        assertThat(CategoryController.CategoryResponse.from(parent).parentId()).isNull();
    }

    @Test
    void productResponsesHandleCategoryAndRequestOverloads() {
        Category category = new Category(7L, "Shoes", null, null);
        ReflectionTestUtils.setField(category, "id", 10L);
        Product product = new Product(7L, "Trainer", "Daily shoe", new BigDecimal("50.00"), category);
        ReflectionTestUtils.setField(product, "id", 11L);

        ProductController.ProductRequest request =
                new ProductController.ProductRequest("Trainer", "Daily shoe", new BigDecimal("50.00"), 10L);
        ProductController.ProductResponse response = ProductController.ProductResponse.from(product);

        assertThat(request.active()).isTrue();
        assertThat(response.categoryId()).isEqualTo(10L);
        product.setCategory(null);
        assertThat(ProductController.ProductResponse.from(product).categoryId()).isNull();
    }

    @Test
    void variantAndInventoryResponsesExposeComputedInventory() {
        Product product = new Product(7L, "Trainer", null, new BigDecimal("50.00"), null);
        ReflectionTestUtils.setField(product, "id", 11L);
        ProductVariant variant = new ProductVariant(7L, product, "SKU-1", "42", "Black", null);
        ReflectionTestUtils.setField(variant, "id", 22L);
        Inventory inventory = new Inventory(7L, variant, 10);
        inventory.setReservedQuantity(3);
        ReflectionTestUtils.setField(inventory, "id", 33L);

        ProductVariantController.VariantRequest request =
                new ProductVariantController.VariantRequest("SKU-1", "42", "Black", null, 10);
        ProductVariantController.VariantResponse response =
                ProductVariantController.VariantResponse.from(variant, inventory);
        ProductVariantController.InventoryResponse inventoryResponse =
                ProductVariantController.InventoryResponse.from(inventory);

        assertThat(request.sku()).isEqualTo("SKU-1");
        assertThat(response.productId()).isEqualTo(11L);
        assertThat(inventoryResponse.availableQuantity()).isEqualTo(7);
    }
}
