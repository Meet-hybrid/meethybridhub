package com.meethybridhub.discovery;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.User;
import com.meethybridhub.store.Store;
import com.meethybridhub.store.StoreRepository;
import com.meethybridhub.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final StoreReviewRepository storeReviewRepo;
    private final ProductReviewRepository productReviewRepo;
    private final UserFavoriteRepository favoriteRepo;
    private final FeaturedContentRepository featuredRepo;
    private final StoreRepository storeRepository;
    private final StoreService storeService;

    public DiscoveryService(StoreReviewRepository storeReviewRepo,
                             ProductReviewRepository productReviewRepo,
                             UserFavoriteRepository favoriteRepo,
                             FeaturedContentRepository featuredRepo,
                             StoreRepository storeRepository,
                             StoreService storeService) {
        this.storeReviewRepo = storeReviewRepo;
        this.productReviewRepo = productReviewRepo;
        this.favoriteRepo = favoriteRepo;
        this.featuredRepo = featuredRepo;
        this.storeRepository = storeRepository;
        this.storeService = storeService;
    }

    // ─── Store Reviews ──────────────────────────────────────────────

    public StoreReview createStoreReview(User customer, Long storeId, short rating,
                                          String title, String comment) {
        validateRating(rating);

        if (storeReviewRepo.findByStoreIdAndCustomerId(storeId, customer.getId()).isPresent()) {
            throw new BadRequestException("You have already reviewed this store");
        }

        StoreReview review = new StoreReview(storeId, customer, rating, title, comment, false);
        StoreReview saved = storeReviewRepo.save(review);
        log.info("Store review created: {} for store {} by user {}", saved.getId(), storeId, customer.getId());
        return saved;
    }

    public List<StoreReview> listStoreReviews(Long storeId) {
        return storeReviewRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public Map<String, Object> getStoreReviewSummary(Long storeId) {
        Double avg = storeReviewRepo.findAverageRatingByStoreId(storeId);
        long count = storeReviewRepo.countByStoreId(storeId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : null);
        summary.put("totalReviews", count);
        return summary;
    }

    // ─── Product Reviews ────────────────────────────────────────────

    public ProductReview createProductReview(User customer, Long storeId, Long productId,
                                              short rating, String title, String comment) {
        validateRating(rating);

        if (productReviewRepo.findByProductIdAndCustomerId(productId, customer.getId()).isPresent()) {
            throw new BadRequestException("You have already reviewed this product");
        }

        ProductReview review = new ProductReview(storeId, productId, customer, rating, title, comment, false);
        ProductReview saved = productReviewRepo.save(review);
        log.info("Product review created: {} for product {} by user {}", saved.getId(), productId, customer.getId());
        return saved;
    }

    public List<ProductReview> listProductReviews(Long productId) {
        return productReviewRepo.findByProductIdOrderByCreatedAtDesc(productId);
    }

    // ─── Favorites ──────────────────────────────────────────────────

    public UserFavorite addFavorite(User user, FavoriteEntityType entityType, Long entityId) {
        if (favoriteRepo.existsByUserIdAndEntityTypeAndEntityId(user.getId(), entityType, entityId)) {
            throw new BadRequestException("Already favorited");
        }

        UserFavorite fav = new UserFavorite(user.getId(), entityType, entityId);
        UserFavorite saved = favoriteRepo.save(fav);
        log.info("Favorite added: {} {} by user {}", entityType, entityId, user.getId());
        return saved;
    }

    public void removeFavorite(User user, FavoriteEntityType entityType, Long entityId) {
        favoriteRepo.deleteByUserIdAndEntityTypeAndEntityId(user.getId(), entityType, entityId);
        log.info("Favorite removed: {} {} by user {}", entityType, entityId, user.getId());
    }

    public boolean isFavorited(User user, FavoriteEntityType entityType, Long entityId) {
        return favoriteRepo.existsByUserIdAndEntityTypeAndEntityId(user.getId(), entityType, entityId);
    }

    public List<UserFavorite> listFavorites(User user, FavoriteEntityType entityType) {
        return favoriteRepo.findByUserIdAndEntityTypeOrderByCreatedAtDesc(user.getId(), entityType);
    }

    // ─── Featured Content ───────────────────────────────────────────

    public List<FeaturedContent> getFeaturedStores() {
        return featuredRepo.findActiveByType(FeaturedContentType.STORE, Instant.now());
    }

    public List<FeaturedContent> getFeaturedProducts() {
        return featuredRepo.findActiveByType(FeaturedContentType.PRODUCT, Instant.now());
    }

    public List<FeaturedContent> getActiveBanners() {
        return featuredRepo.findActiveByType(FeaturedContentType.BANNER, Instant.now());
    }

    public List<FeaturedContent> getAllFeatured() {
        return featuredRepo.findAllActive(Instant.now());
    }

    // ─── Discovery / Search ─────────────────────────────────────────

    public List<Store> searchStores(String query, int page, int size) {
        // Simple LIKE search on store name/description
        if (query == null || query.isBlank()) {
            return storeRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                    .getContent();
        }
        // Use a native query via store name pattern for now
        return storeRepository.findAll(PageRequest.of(page, size)).getContent().stream()
                .filter(s -> s.getName().toLowerCase().contains(query.toLowerCase())
                        || (s.getDescription() != null && s.getDescription().toLowerCase().contains(query.toLowerCase())))
                .toList();
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private void validateRating(short rating) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }
}
