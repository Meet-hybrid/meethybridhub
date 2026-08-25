package com.meethybridhub.catalog;

import com.meethybridhub.catalog.Category;
import com.meethybridhub.catalog.CategoryRepository;
import com.meethybridhub.catalog.Product;
import com.meethybridhub.catalog.ProductRepository;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product create(Long storeId, String name, String description, BigDecimal price, Long categoryId) {
        Category category = categoryId == null ? null : categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        return productRepository.save(new Product(storeId, name.trim(), blankToNull(description), price, category));
    }

    @Transactional(readOnly = true)
    public Page<Product> list(Long storeId, Pageable pageable, boolean activeOnly) {
        return activeOnly
                ? productRepository.findAllByStoreIdAndActiveTrue(storeId, pageable)
                : productRepository.findAllByStoreId(storeId, pageable);
    }

    @Transactional(readOnly = true)
    public Product get(Long storeId, Long productId) {
        return productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    public Product update(Long storeId, Long productId, String name, String description,
                          BigDecimal price, Long categoryId, boolean active) {
        Product product = get(storeId, productId);
        Category category = categoryId == null ? null : categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        product.setName(name.trim());
        product.setDescription(blankToNull(description));
        product.setPrice(price);
        product.setCategory(category);
        product.setActive(active);
        return productRepository.save(product);
    }

    public void delete(Long storeId, Long productId) {
        Product product = get(storeId, productId);
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public Page<Product> search(Long storeId, Pageable pageable, boolean activeOnly, String search,
                                Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new com.meethybridhub.common.exception.BadRequestException("Minimum price cannot exceed maximum price");
        }
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return productRepository.search(storeId, activeOnly, normalizedSearch, categoryId, minPrice, maxPrice, pageable);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
