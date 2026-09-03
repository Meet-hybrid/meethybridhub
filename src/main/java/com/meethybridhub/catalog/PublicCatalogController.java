package com.meethybridhub.catalog;

import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.store.Store;
import com.meethybridhub.store.StoreRepository;
import com.meethybridhub.store.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** Public, read-only storefront API. Every query is explicitly scoped by store slug. */
@RestController
@RequestMapping("/api/v1/public/stores/{slug}")
public class PublicCatalogController {

    private final StoreRepository storeRepository;
    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    public PublicCatalogController(StoreRepository storeRepository,
                                   ProductService productService,
                                   CategoryRepository categoryRepository) {
        this.storeRepository = storeRepository;
        this.productService = productService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<StoreSummary> store(@PathVariable String slug) {
        Store store = activeStore(slug);
        return ResponseEntity.ok(new StoreSummary(store.getId(), store.getName(), store.getSlug(), store.getDescription()));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryController.CategoryResponse>> categories(@PathVariable String slug) {
        Store store = activeStore(slug);
        return ResponseEntity.ok(categoryRepository.findAllByStoreId(store.getId()).stream()
                .map(category -> new CategoryController.CategoryResponse(
                        category.getId(), category.getStoreId(), category.getName(),
                        category.getDescription(), category.getParentCategory() == null ? null : category.getParentCategory().getId()))
                .toList());
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductController.ProductResponse>> products(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        Store store = activeStore(slug);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Product> result = productService.search(store.getId(),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")),
                true, search, categoryId, minPrice, maxPrice);
        return ResponseEntity.ok(result.map(ProductController.ProductResponse::from));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductController.ProductResponse> product(@PathVariable String slug,
                                                                        @PathVariable Long productId) {
        Store store = activeStore(slug);
        return ResponseEntity.ok(ProductController.ProductResponse.from(productService.get(store.getId(), productId)));
    }

    private Store activeStore(String slug) {
        Store store = storeRepository.findBySlug(slug.toLowerCase()).orElseThrow(
                () -> new ResourceNotFoundException("Store not found: " + slug));
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ResourceNotFoundException("Store is not available: " + slug);
        }
        return store;
    }

    public record StoreSummary(Long id, String name, String slug, String description) {}
}
