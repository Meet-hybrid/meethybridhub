package com.meethybridhub.catalog;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.StoreService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
public class ProductController {

    private final ProductService productService;
    private final StoreService storeService;
    private final UserService userService;

    public ProductController(ProductService productService, StoreService storeService, UserService userService) {
        this.productService = productService;
        this.storeService = storeService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductRequest request) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        Product product = productService.create(TenantContext.requireStoreId(), request.name(), request.description(), request.price(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        return ResponseEntity.ok(productService.search(TenantContext.requireStoreId(), pageable, activeOnly, null, null, null, null).map(ProductResponse::from));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> get(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        authorize(userDetails);
        return ResponseEntity.ok(ProductResponse.from(productService.get(TenantContext.requireStoreId(), productId)));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> search(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        authorize(userDetails);
        return ResponseEntity.ok(productService.search(TenantContext.requireStoreId(), pageable, activeOnly,
                search, categoryId, minPrice, maxPrice).map(ProductResponse::from));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) {
        authorize(userDetails);
        Product product = productService.update(TenantContext.requireStoreId(), productId, request.name(), request.description(),
                request.price(), request.categoryId(), request.active());
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        authorize(userDetails);
        productService.delete(TenantContext.requireStoreId(), productId);
        return ResponseEntity.noContent().build();
    }

    private long authorize(UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        return TenantContext.requireStoreId();
    }

    public record ProductRequest(
            @NotBlank(message = "Product name is required")
            @Size(max = 200, message = "Product name must be at most 200 characters")
            String name,
            @Size(max = 5000, message = "Product description must be at most 5000 characters")
            String description,
            @NotNull(message = "Product price is required")
            @DecimalMin(value = "0.00", message = "Product price cannot be negative")
            @Digits(integer = 17, fraction = 2, message = "Product price must have at most 2 decimal places")
            BigDecimal price,
            Long categoryId,
            boolean active) {
        public ProductRequest(String name, String description, BigDecimal price, Long categoryId) {
            this(name, description, price, categoryId, true);
        }
    }

    public record ProductResponse(Long id, Long storeId, String name, String description,
                                  BigDecimal price, Long categoryId, boolean active) {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getStoreId(), product.getName(), product.getDescription(),
                    product.getPrice(), product.getCategory() == null ? null : product.getCategory().getId(), product.isActive());
        }
    }
}
