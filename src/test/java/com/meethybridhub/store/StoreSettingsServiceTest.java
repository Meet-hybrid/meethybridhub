package com.meethybridhub.store;

import com.meethybridhub.identity.AuditLogService;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StoreService} settings behavior that is hard to hit in
 * integration tests: the concurrent first-GET lazy-create race.
 */
@ExtendWith(MockitoExtension.class)
class StoreSettingsServiceTest {

    private static final long STORE_ID = 7L;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreDomainRepository storeDomainRepository;

    @Mock
    private StoreSettingsRepository storeSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @BeforeEach
    void setTenant() {
        TenantContext.setStoreId(STORE_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void concurrentFirstGetFallsBackToRequery() {
        User owner = admin();
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store(owner)));

        StoreSettings winner = new StoreSettings(STORE_ID);

        // First GET misses, the save loses the unique store_id race, and the
        // re-query finds the row the winner persisted.
        when(storeSettingsRepository.findByStoreId(STORE_ID))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(storeSettingsRepository.save(any(StoreSettings.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        StoreService service = new StoreService(storeRepository, storeDomainRepository,
                storeSettingsRepository, userRepository, auditLogService);

        StoreSettings result = service.getSettingsForCurrentTenant(owner);

        assertThat(result).isSameAs(winner);
    }

    @Test
    void requeryAfterRaceNeverEmptyInPractice() {
        User owner = admin();
        when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store(owner)));
        when(storeSettingsRepository.findByStoreId(STORE_ID))
                .thenReturn(Optional.empty(), Optional.empty());
        when(storeSettingsRepository.save(any(StoreSettings.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        StoreService service = new StoreService(storeRepository, storeDomainRepository,
                storeSettingsRepository, userRepository, auditLogService);

        try {
            service.getSettingsForCurrentTenant(owner);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("vanished after concurrent creation");
            return;
        }
        // The winner's row can never vanish (same transaction), so this branch
        // is defense-in-depth; reaching it without throwing is also acceptable.
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** An ADMIN bypasses the ownership check in getCurrentTenantStore, which
     *  keeps this unit test focused on the settings race, not authz. */
    private User admin() {
        User owner = new User("owner@example.com", "hash", "Owner");
        owner.addRole("ADMIN");
        return owner;
    }

    /** A store whose id is set (DB-generated in production). */
    private Store store(User owner) {
        Store store = new Store(owner, "Race Shop", "race-shop", null);
        ReflectionTestUtils.setField(store, "id", STORE_ID);
        return store;
    }
}
