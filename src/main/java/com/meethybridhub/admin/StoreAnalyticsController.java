package com.meethybridhub.admin;

import com.meethybridhub.identity.User;
import com.meethybridhub.identity.UserService;
import com.meethybridhub.store.StoreService;
import com.meethybridhub.store.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores/me/analytics")
@PreAuthorize("hasAnyRole('STORE_OWNER', 'ADMIN')")
public class StoreAnalyticsController {
    private final AdminService adminService;
    private final StoreService storeService;
    private final UserService userService;

    public StoreAnalyticsController(AdminService adminService, StoreService storeService, UserService userService) {
        this.adminService = adminService;
        this.storeService = storeService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> analytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        storeService.getCurrentTenantStore(user);
        return ResponseEntity.ok(adminService.getStoreAnalytics(TenantContext.requireStoreId(), days));
    }
}
