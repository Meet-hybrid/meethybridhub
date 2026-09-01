package com.meethybridhub.discovery;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.identity.User;
import com.meethybridhub.store.StoreRepository;
import com.meethybridhub.store.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock private StoreReviewRepository storeReviewRepo;
    @Mock private ProductReviewRepository productReviewRepo;
    @Mock private UserFavoriteRepository favoriteRepo;
    @Mock private FeaturedContentRepository featuredRepo;
    @Mock private StoreRepository storeRepository;
    @Mock private StoreService storeService;

    @InjectMocks private DiscoveryService service;

    private User customer;

    @BeforeEach
    void setUp() throws Exception {
        customer = new User("test@test.com", "hash", "Test User");
        customer.setRoles("CUSTOMER");
        var idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, 10L);
    }

    // ─── Store Reviews ──────────────────────────────────────────────

    @Test
    void createStoreReview_savesAndReturns() {
        when(storeReviewRepo.findByStoreIdAndCustomerId(1L, 10L)).thenReturn(Optional.empty());
        when(storeReviewRepo.save(any())).thenAnswer(i -> {
            StoreReview r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        StoreReview review = service.createStoreReview(customer, 1L, (short) 5, "Great store", "Loved it");

        assertNotNull(review);
        assertEquals(5, review.getRating());
        assertEquals("Great store", review.getTitle());
    }

    @Test
    void createStoreReview_rejectsDuplicate() {
        when(storeReviewRepo.findByStoreIdAndCustomerId(1L, 10L))
                .thenReturn(Optional.of(new StoreReview(1L, customer, (short) 4, "Old", "Old review", false)));

        assertThrows(BadRequestException.class,
                () -> service.createStoreReview(customer, 1L, (short) 5, "New", "New review"));
    }

    @Test
    void createStoreReview_rejectsInvalidRating() {
        assertThrows(BadRequestException.class,
                () -> service.createStoreReview(customer, 1L, (short) 0, "Title", "Comment"));
        assertThrows(BadRequestException.class,
                () -> service.createStoreReview(customer, 1L, (short) 6, "Title", "Comment"));
    }

    @Test
    void listStoreReviews_returnsList() {
        StoreReview r = new StoreReview(1L, customer, (short) 5, "Great", "Love", false);
        when(storeReviewRepo.findByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r));

        List<StoreReview> result = service.listStoreReviews(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getStoreReviewSummary_returnsAverageAndCount() {
        when(storeReviewRepo.findAverageRatingByStoreId(1L)).thenReturn(4.5);
        when(storeReviewRepo.countByStoreId(1L)).thenReturn(10L);

        var summary = service.getStoreReviewSummary(1L);
        assertEquals(4.5, summary.get("averageRating"));
        assertEquals(10L, summary.get("totalReviews"));
    }

    // ─── Product Reviews ────────────────────────────────────────────

    @Test
    void createProductReview_savesAndReturns() {
        when(productReviewRepo.findByProductIdAndCustomerId(5L, 10L)).thenReturn(Optional.empty());
        when(productReviewRepo.save(any())).thenAnswer(i -> {
            ProductReview r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        ProductReview review = service.createProductReview(
                customer, 1L, 5L, (short) 4, "Nice product", "Good quality");

        assertNotNull(review);
        assertEquals(4, review.getRating());
        assertEquals(5L, review.getProductId());
    }

    @Test
    void createProductReview_rejectsDuplicate() {
        when(productReviewRepo.findByProductIdAndCustomerId(5L, 10L))
                .thenReturn(Optional.of(new ProductReview(1L, 5L, customer, (short) 3, "Old", "Old", false)));

        assertThrows(BadRequestException.class,
                () -> service.createProductReview(customer, 1L, 5L, (short) 5, "New", "New"));
    }

    // ─── Favorites ──────────────────────────────────────────────────

    @Test
    void addFavorite_savesWhenNotExists() {
        when(favoriteRepo.existsByUserIdAndEntityTypeAndEntityId(10L, FavoriteEntityType.STORE, 1L))
                .thenReturn(false);
        when(favoriteRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UserFavorite fav = service.addFavorite(customer, FavoriteEntityType.STORE, 1L);
        assertNotNull(fav);
        assertEquals(FavoriteEntityType.STORE, fav.getEntityType());
    }

    @Test
    void addFavorite_rejectsDuplicate() {
        when(favoriteRepo.existsByUserIdAndEntityTypeAndEntityId(10L, FavoriteEntityType.STORE, 1L))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.addFavorite(customer, FavoriteEntityType.STORE, 1L));
    }

    @Test
    void removeFavorite_deletes() {
        service.removeFavorite(customer, FavoriteEntityType.PRODUCT, 5L);
        verify(favoriteRepo).deleteByUserIdAndEntityTypeAndEntityId(10L, FavoriteEntityType.PRODUCT, 5L);
    }

    @Test
    void isFavorited_delegatesToRepo() {
        when(favoriteRepo.existsByUserIdAndEntityTypeAndEntityId(10L, FavoriteEntityType.STORE, 1L))
                .thenReturn(true);
        assertTrue(service.isFavorited(customer, FavoriteEntityType.STORE, 1L));
    }

    // ─── Featured Content ───────────────────────────────────────────

    @Test
    void getFeaturedStores_queriesActive() {
        when(featuredRepo.findActiveByType(eq(FeaturedContentType.STORE), any())).thenReturn(List.of());
        assertEquals(0, service.getFeaturedStores().size());
    }

    @Test
    void getActiveBanners_queriesActive() {
        when(featuredRepo.findActiveByType(eq(FeaturedContentType.BANNER), any())).thenReturn(List.of());
        assertEquals(0, service.getActiveBanners().size());
    }
}
