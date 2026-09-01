package com.meethybridhub.discovery;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.Store;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {

    private final DiscoveryService service;
    private final UserService userService;

    public DiscoveryController(DiscoveryService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    // ─── Store Reviews ──────────────────────────────────────────────

    @PostMapping("/stores/{storeId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StoreReviewResponse> createStoreReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long storeId,
            @Valid @RequestBody CreateReviewRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        StoreReview review = service.createStoreReview(
                user, storeId, body.rating(), body.title(), body.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(StoreReviewResponse.from(review));
    }

    @GetMapping("/stores/{storeId}/reviews")
    public ResponseEntity<List<StoreReviewResponse>> listStoreReviews(@PathVariable Long storeId) {
        return ResponseEntity.ok(
                service.listStoreReviews(storeId).stream()
                        .map(StoreReviewResponse::from).toList());
    }

    @GetMapping("/stores/{storeId}/reviews/summary")
    public ResponseEntity<Map<String, Object>> storeReviewSummary(@PathVariable Long storeId) {
        return ResponseEntity.ok(service.getStoreReviewSummary(storeId));
    }

    // ─── Product Reviews ────────────────────────────────────────────

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductReviewResponse> createProductReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductReviewRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        ProductReview review = service.createProductReview(
                user, body.storeId(), productId, body.rating(), body.title(), body.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductReviewResponse.from(review));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ProductReviewResponse>> listProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(
                service.listProductReviews(productId).stream()
                        .map(ProductReviewResponse::from).toList());
    }

    // ─── Favorites ──────────────────────────────────────────────────

    @PostMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> addFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddFavoriteRequest body) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        service.addFavorite(user, body.entityType(), body.entityId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Added to favorites"));
    }

    @DeleteMapping("/favorites/{entityType}/{entityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> removeFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable FavoriteEntityType entityType,
            @PathVariable Long entityId) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        service.removeFavorite(user, entityType, entityId);
        return ResponseEntity.ok(Map.of("message", "Removed from favorites"));
    }

    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserFavorite>> listFavorites(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam FavoriteEntityType entityType) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(service.listFavorites(user, entityType));
    }

    // ─── Featured Content ───────────────────────────────────────────

    @GetMapping("/featured/stores")
    public ResponseEntity<List<FeaturedContent>> featuredStores() {
        return ResponseEntity.ok(service.getFeaturedStores());
    }

    @GetMapping("/featured/products")
    public ResponseEntity<List<FeaturedContent>> featuredProducts() {
        return ResponseEntity.ok(service.getFeaturedProducts());
    }

    @GetMapping("/featured/banners")
    public ResponseEntity<List<FeaturedContent>> featuredBanners() {
        return ResponseEntity.ok(service.getActiveBanners());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<FeaturedContent>> allFeatured() {
        return ResponseEntity.ok(service.getAllFeatured());
    }

    // ─── Search ─────────────────────────────────────────────────────

    @GetMapping("/search/stores")
    public ResponseEntity<List<Store>> searchStores(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.searchStores(q, page, size));
    }

    // ─── DTOs ───────────────────────────────────────────────────────

    public record CreateReviewRequest(
            @NotNull @Min(1) @Max(5) short rating,
            String title,
            String comment) {}

    public record CreateProductReviewRequest(
            @NotNull Long storeId,
            @NotNull @Min(1) @Max(5) short rating,
            String title,
            String comment) {}

    public record AddFavoriteRequest(
            @NotNull FavoriteEntityType entityType,
            @NotNull Long entityId) {}

    public record StoreReviewResponse(
            Long id, Long customerId, short rating, String title,
            String comment, boolean verified, String createdAt) {

        public static StoreReviewResponse from(StoreReview r) {
            return new StoreReviewResponse(
                    r.getId(), r.getCustomerId(), r.getRating(),
                    r.getTitle(), r.getComment(), r.isVerified(),
                    r.getCreatedAt().toString());
        }
    }

    public record ProductReviewResponse(
            Long id, Long productId, Long customerId, short rating,
            String title, String comment, boolean verified, String createdAt) {

        public static ProductReviewResponse from(ProductReview r) {
            return new ProductReviewResponse(
                    r.getId(), r.getProductId(), r.getCustomerId(), r.getRating(),
                    r.getTitle(), r.getComment(), r.isVerified(),
                    r.getCreatedAt().toString());
        }
    }
}
