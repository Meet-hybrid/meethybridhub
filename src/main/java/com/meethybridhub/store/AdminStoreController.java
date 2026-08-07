package com.meethybridhub.store;

import com.meethybridhub.identity.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin store management (the remaining Card 3 scope).
 *
 *   GET /api/v1/admin/stores             - list stores (filter by status)
 *   PUT /api/v1/admin/stores/{id}/status - set a store's lifecycle status
 *
 * Protected twice: the URL rule ({@code hasRole('ADMIN')} in SecurityConfig)
 * and the class-level {@code @PreAuthorize} below (defense in depth). Stores
 * are platform-level entities (they ARE the tenants), so no tenant context is
 * involved here.
 */
@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final StoreService storeService;

    public AdminStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<List<StoreController.StoreResponse>> listStores(
            @RequestParam(required = false) String status) {
        List<StoreController.StoreResponse> stores = storeService.listStores(status).stream()
                .map(StoreController.StoreResponse::from)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<StoreController.StoreResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoreStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Store updated = storeService.updateStoreStatus(
                ((AppUser) userDetails).getUser().getId(), id, request.status());
        return ResponseEntity.ok(StoreController.StoreResponse.from(updated));
    }

    public record UpdateStoreStatusRequest(
            @NotNull(message = "Status is required")
            StoreStatus status
    ) {}
}
