package com.meethybridhub.catalog;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.StoreService;
import com.meethybridhub.store.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
public class ProductVariantController {

    private final ProductVariantService variantService;
    private final StoreService storeService;
    private final UserService userService;

    public ProductVariantController(ProductVariantService variantService, StoreService storeService, UserService userService) {
        this.variantService = variantService;
        this.storeService = storeService;
        this.userService = userService;
    }

    @PostMapping("/api/v1/products/{productId}/variants")
    public ResponseEntity<VariantResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody VariantRequest request) {
        long storeId = authorize(userDetails);
        ProductVariant variant = variantService.create(storeId, productId, request.sku(), request.size(),
                request.color(), request.priceOverride(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(VariantResponse.from(variant, variantService.getInventory(storeId, variant.getId())));
    }

    @GetMapping("/api/v1/products/{productId}/variants")
    public ResponseEntity<List<VariantResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        long storeId = authorize(userDetails);
        return ResponseEntity.ok(variantService.list(storeId, productId).stream()
                .map(variant -> VariantResponse.from(variant, variantService.getInventory(storeId, variant.getId())))
                .toList());
    }

    @GetMapping("/api/v1/variants/{variantId}/inventory")
    public ResponseEntity<InventoryResponse> getInventory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long variantId) {
        long storeId = authorize(userDetails);
        return ResponseEntity.ok(InventoryResponse.from(variantService.getInventory(storeId, variantId)));
    }

    @PutMapping("/api/v1/variants/{variantId}/inventory")
    public ResponseEntity<InventoryResponse> updateInventory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long variantId,
            @Valid @RequestBody InventoryRequest request) {
        long storeId = authorize(userDetails);
        return ResponseEntity.ok(InventoryResponse.from(variantService.updateInventory(
                storeId, variantId, request.quantity(), request.reservedQuantity())));
    }

    private long authorize(UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        return TenantContext.requireStoreId();
    }

    public record VariantRequest(
            @NotBlank(message = "SKU is required")
            @Size(max = 100, message = "SKU must be at most 100 characters")
            String sku,
            @Size(max = 80, message = "Size must be at most 80 characters") String size,
            @Size(max = 80, message = "Color must be at most 80 characters") String color,
            @DecimalMin(value = "0.00", message = "Price override cannot be negative")
            @Digits(integer = 17, fraction = 2, message = "Price override must have at most 2 decimal places")
            BigDecimal priceOverride,
            @Min(value = 0, message = "Quantity cannot be negative") int quantity) {}

    public record InventoryRequest(
            @Min(value = 0, message = "Quantity cannot be negative") int quantity,
            @Min(value = 0, message = "Reserved quantity cannot be negative") int reservedQuantity) {}

    public record VariantResponse(Long id, Long productId, String sku, String size, String color,
                                  BigDecimal priceOverride, boolean active, InventoryResponse inventory) {
        static VariantResponse from(ProductVariant variant, Inventory inventory) {
            return new VariantResponse(variant.getId(), variant.getProduct().getId(), variant.getSku(), variant.getSize(),
                    variant.getColor(), variant.getPriceOverride(), variant.isActive(), InventoryResponse.from(inventory));
        }
    }

    public record InventoryResponse(Long id, Long variantId, int quantity, int reservedQuantity, int availableQuantity) {
        static InventoryResponse from(Inventory inventory) {
            return new InventoryResponse(inventory.getId(), inventory.getVariant().getId(), inventory.getQuantity(),
                    inventory.getReservedQuantity(), inventory.getAvailableQuantity());
        }
    }
}
