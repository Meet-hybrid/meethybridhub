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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
@Transactional
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    private final StoreRepository storeRepository;
    private final StoreDomainRepository storeDomainRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public StoreService(StoreRepository storeRepository,
                        StoreDomainRepository storeDomainRepository,
                        StoreSettingsRepository storeSettingsRepository,
                        UserRepository userRepository,
                        AuditLogService auditLogService) {
        this.storeRepository = storeRepository;
        this.storeDomainRepository = storeDomainRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }


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


    public Optional<Long> findActiveStoreIdForOwner(Long userId) {
        return storeRepository.findByOwnerIdAndStatus(userId, StoreStatus.ACTIVE)
                .map(Store::getId);
    }


    public Store getCurrentTenantStore(User user) {
        long storeId = TenantContext.requireStoreId();
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));

        if (!user.hasRole("ADMIN") && !store.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have access to store: " + storeId);
        }
        return store;
    }


    public List<StoreDomain> getDomainsForCurrentTenant(User user) {
        Store store = getCurrentTenantStore(user);
        return storeDomainRepository.findAllByStoreId(store.getId());
    }


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


    public StoreSettings getSettingsForCurrentTenant(User user) {
        Store store = getCurrentTenantStore(user);
        return findOrCreateSettings(store.getId());
    }

    private StoreSettings findOrCreateSettings(Long storeId) {
        return storeSettingsRepository.findByStoreId(storeId)
                .orElseGet(() -> {
                    try {
                        return storeSettingsRepository.save(new StoreSettings(storeId));
                    } catch (DataIntegrityViolationException e) {


                        return storeSettingsRepository.findByStoreId(storeId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Store settings vanished after concurrent creation: " + storeId));
                    }
                });
    }


    public StoreSettings updateSettingsForCurrentTenant(User user, StoreSettingsUpdate update) {
        Store store = getCurrentTenantStore(user);
        StoreSettings settings = storeSettingsRepository.findByStoreId(store.getId())
                .orElseGet(() -> new StoreSettings(store.getId()));

        if (update.logoUrl() != null) {
            settings.setLogoUrl(blankToNull(update.logoUrl()));
        }
        if (update.primaryColor() != null) {
            settings.setPrimaryColor(update.primaryColor().trim());
        }
        if (update.accentColor() != null) {
            settings.setAccentColor(update.accentColor().trim());
        }
        if (update.theme() != null) {
            settings.setTheme(update.theme());
        }
        if (update.tagline() != null) {
            settings.setTagline(blankToNull(update.tagline()));
        }
        if (update.contactEmail() != null) {
            settings.setContactEmail(blankToNull(update.contactEmail()));
        }

        StoreSettings saved = storeSettingsRepository.save(settings);
        auditLogService.record(user.getId(), AuditEventType.STORE_SETTINGS_UPDATED,
                "Store " + store.getSlug() + " branding/settings updated", null, null);
        return saved;
    }


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
