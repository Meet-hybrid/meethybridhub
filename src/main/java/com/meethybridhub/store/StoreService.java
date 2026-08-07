package com.meethybridhub.store;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ForbiddenException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import com.meethybridhub.identity.AuditEventType;
import com.meethybridhub.identity.AuditLogService;
import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Business logic for stores and store-scoped data.
 *
 * The key method is {@link #getCurrentTenantStore(User)}: it combines the
 * tenant resolved by {@link StoreFilter} (via {@link TenantContext}) with the
 * authenticated user, enforcing that a store owner can only ever reach their
 * OWN store (admins may access any store). Every other store-scoped operation
 * funnels through it — that is the isolation guarantee at the application layer.
 */
@Service
@Transactional
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    private final StoreRepository storeRepository;
    private final StoreDomainRepository storeDomainRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public StoreService(StoreRepository storeRepository,
                        StoreDomainRepository storeDomainRepository,
                        UserRepository userRepository,
                        AuditLogService auditLogService) {
        this.storeRepository = storeRepository;
        this.storeDomainRepository = storeDomainRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Register a new store owned by {@code owner}. The owner is granted the
     * STORE_OWNER role on first store creation.
     */
    public Store createStore(User owner, String name, String description) {
        String slug = uniqueSlug(name);

        Store store = new Store(owner, name.trim(), slug, blankToNull(description));
        store.setStatus(StoreStatus.ACTIVE);
        Store saved = storeRepository.save(store);

        if (!owner.hasRole("STORE_OWNER")) {
            owner.addRole("STORE_OWNER");
            userRepository.save(owner);
        }

        log.info("Store created: {} (slug: {}, owner: {})", saved.getName(), saved.getSlug(), owner.getEmail());
        auditLogService.record(owner.getId(), AuditEventType.STORE_CREATED,
                "Store created: " + saved.getSlug(), null, null);
        return saved;
    }

    /**
     * The ID of the active store owned by {@code userId}, if any. Used at JWT
     * issuance so tokens carry a {@code storeId} claim for tenant resolution.
     */
    public Optional<Long> findActiveStoreIdForOwner(Long userId) {
        return storeRepository.findByOwnerIdAndStatus(userId, StoreStatus.ACTIVE)
                .map(Store::getId);
    }

    /**
     * The store the current request operates on — the tenant from
     * {@link TenantContext}, checked against the authenticated user.
     *
     * @throws ForbiddenException if the user is neither the store's owner nor an admin
     */
    public Store getCurrentTenantStore(User user) {
        long storeId = TenantContext.requireStoreId();
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));

        if (!user.hasRole("ADMIN") && !store.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to store: " + storeId);
        }
        return store;
    }

    /** All domains of the current tenant store (tenant-scoped read). */
    public List<StoreDomain> getDomainsForCurrentTenant(User user) {
        Store store = getCurrentTenantStore(user);
        return storeDomainRepository.findAllByStoreId(store.getId());
    }

    /** Register a new domain for the current tenant store. */
    public StoreDomain addDomain(User user, String domain) {
        Store store = getCurrentTenantStore(user);
        String normalized = normalizeDomain(domain);

        if (storeDomainRepository.existsByDomain(normalized)) {
            throw new BadRequestException("Domain already registered: " + normalized);
        }

        boolean isFirst = storeDomainRepository.countByStoreId(store.getId()) == 0;
        StoreDomain saved = storeDomainRepository.save(
                new StoreDomain(store.getId(), normalized, isFirst, false));
        log.info("Domain {} added to store {}", saved.getDomain(), store.getSlug());
        return saved;
    }

    /**
     * List all stores, optionally filtered by status (admin only).
     *
     * @throws BadRequestException for an unknown status value
     */
    public List<Store> listStores(String status) {
        if (status != null && !status.isBlank()) {
            try {
                return storeRepository.findByStatus(StoreStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown status: " + status
                        + ". Valid values: " + Arrays.toString(StoreStatus.values()));
            }
        }
        return storeRepository.findAll();
    }

    /**
     * Set a store's lifecycle status (admin only). The acting admin is recorded
     * in the audit trail so status changes are attributable.
     */
    public Store updateStoreStatus(Long actorUserId, Long storeId, StoreStatus status) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        StoreStatus previous = store.getStatus();
        store.setStatus(status);
        Store saved = storeRepository.save(store);
        auditLogService.record(actorUserId, AuditEventType.STORE_STATUS_UPDATED,
                "Store " + storeId + " (" + saved.getSlug() + ") status changed from " + previous + " to " + status,
                null, null);
        return saved;
    }

    private String uniqueSlug(String name) {
        String base = slugify(name);
        if (base.isEmpty()) {
            throw new BadRequestException("Store name must contain letters or numbers");
        }
        String slug = base;
        int suffix = 1;
        while (storeRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    /** "Divine Signature" -> "divine-signature" */
    private String slugify(String name) {
        return name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("^https?://", "")
                .replaceFirst("/.*$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
