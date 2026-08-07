package com.meethybridhub.store;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Store endpoints.
 *
 *   POST   /api/v1/stores            - create a store (any authenticated user;
 *                                      the creator becomes STORE_OWNER)
 *   GET    /api/v1/stores/me         - the store for the current tenant context
 *   GET    /api/v1/stores/me/domains - domains of the current tenant store
 *   POST   /api/v1/stores/me/domains - register a domain for the current tenant store
 *
 * Store management requires STORE_OWNER or ADMIN. The current tenant is set by
 * the {@code X-Store-Id} header or the store subdomain (see {@link StoreFilter}).
 */
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    public StoreController(StoreService storeService, UserService userService) {
        this.storeService = storeService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StoreResponse> createStore(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateStoreRequest request) {

        User owner = userService.getUserByEmail(userDetails.getUsername());
        Store store = storeService.createStore(owner, request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(StoreResponse.from(store));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<StoreResponse> getMyStore(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(StoreResponse.from(storeService.getCurrentTenantStore(user)));
    }

    @GetMapping("/me/domains")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<List<DomainResponse>> getMyDomains(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<DomainResponse> domains = storeService.getDomainsForCurrentTenant(user).stream()
                .map(DomainResponse::from)
                .toList();
        return ResponseEntity.ok(domains);
    }

    @PostMapping("/me/domains")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<DomainResponse> addDomain(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddDomainRequest request) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        StoreDomain domain = storeService.addDomain(user, request.domain());
        return ResponseEntity.status(HttpStatus.CREATED).body(DomainResponse.from(domain));
    }

    @GetMapping("/me/settings")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<StoreSettingsResponse> getMySettings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(StoreSettingsResponse.from(storeService.getSettingsForCurrentTenant(user)));
    }

    @PutMapping("/me/settings")
    @PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
    public ResponseEntity<StoreSettingsResponse> updateMySettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StoreSettingsUpdate request) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(StoreSettingsResponse.from(storeService.updateSettingsForCurrentTenant(user, request)));
    }

    // Request/Response records

    public record CreateStoreRequest(
            @NotBlank(message = "Store name is required")
            @Size(min = 2, max = 100, message = "Store name must be between 2 and 100 characters")
            String name,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description
    ) {}

    public record AddDomainRequest(
            @NotBlank(message = "Domain is required")
            @Pattern(
                    regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$",
                    message = "Please provide a valid domain (e.g. shop.example.com)")
            String domain
    ) {}

    public record StoreResponse(
            Long id,
            String name,
            String slug,
            String description,
            StoreStatus status,
            String ownerEmail,
            Instant createdAt
    ) {
        public static StoreResponse from(Store store) {
            return new StoreResponse(
                    store.getId(),
                    store.getName(),
                    store.getSlug(),
                    store.getDescription(),
                    store.getStatus(),
                    store.getOwner().getEmail(),
                    store.getCreatedAt()
            );
        }
    }

    public record StoreSettingsResponse(
            Long id,
            Long storeId,
            String logoUrl,
            String primaryColor,
            String accentColor,
            StoreTheme theme,
            String tagline,
            String contactEmail,
            Instant updatedAt
    ) {
        public static StoreSettingsResponse from(StoreSettings settings) {
            return new StoreSettingsResponse(
                    settings.getId(),
                    settings.getStoreId(),
                    settings.getLogoUrl(),
                    settings.getPrimaryColor(),
                    settings.getAccentColor(),
                    settings.getTheme(),
                    settings.getTagline(),
                    settings.getContactEmail(),
                    settings.getUpdatedAt()
            );
        }
    }

    public record DomainResponse(
            Long id,
            Long storeId,
            String domain,
            boolean primary,
            boolean verified
    ) {
        public static DomainResponse from(StoreDomain domain) {
            return new DomainResponse(
                    domain.getId(),
                    domain.getStoreId(),
                    domain.getDomain(),
                    domain.isPrimary(),
                    domain.isVerified()
            );
        }
    }
}
