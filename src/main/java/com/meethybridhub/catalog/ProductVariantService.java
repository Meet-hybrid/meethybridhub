package com.meethybridhub.catalog;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;

    public ProductVariantService(ProductRepository productRepository,
                                 ProductVariantRepository variantRepository,
                                 InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public ProductVariant create(Long storeId, Long productId, String sku, String size,
                                 String color, BigDecimal priceOverride, int quantity) {
        Product product = productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        String normalizedSku = sku.trim();
        if (variantRepository.findByStoreIdAndSku(storeId, normalizedSku).isPresent()) {
            throw new BadRequestException("SKU already exists: " + normalizedSku);
        }
        ProductVariant variant = variantRepository.save(new ProductVariant(
                storeId, product, normalizedSku, blankToNull(size), blankToNull(color), priceOverride));
        inventoryRepository.save(new Inventory(storeId, variant, quantity));
        return variant;
    }

    @Transactional(readOnly = true)
    public List<ProductVariant> list(Long storeId, Long productId) {
        if (!productRepository.existsByIdAndStoreId(productId, storeId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        return variantRepository.findAllByStoreIdAndProductId(storeId, productId);
    }

    public Inventory updateInventory(Long storeId, Long variantId, int quantity, int reservedQuantity) {
        ProductVariant variant = variantRepository.findByIdAndStoreId(variantId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + variantId));
        if (reservedQuantity > quantity) {
            throw new BadRequestException("Reserved quantity cannot exceed total quantity");
        }
        Inventory inventory = inventoryRepository.findByStoreIdAndVariantId(storeId, variantId)
                .orElseGet(() -> new Inventory(storeId, variant, quantity));
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reservedQuantity);
        return inventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public Inventory getInventory(Long storeId, Long variantId) {
        return inventoryRepository.findByStoreIdAndVariantId(storeId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for variant: " + variantId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
