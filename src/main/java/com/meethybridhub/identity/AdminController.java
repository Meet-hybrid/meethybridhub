package com.meethybridhub.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin user management (Hybrid's Card 3).
 *
 *   GET    /api/v1/admin/users              - list users (filter by status/role)
 *   GET    /api/v1/admin/users/{id}         - user detail
 *   PUT    /api/v1/admin/users/{id}/roles   - set a user's roles
 *   PUT    /api/v1/admin/users/{id}/status  - set a user's account status
 *   DELETE /api/v1/admin/users/{id}         - soft-delete a user account
 *
 * Protected twice: URL rule ({@code hasRole('ADMIN')} in SecurityConfig) and
 * the class-level {@code @PreAuthorize} below (defense in depth).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final ClientIpResolver clientIpResolver;

    public AdminController(UserService userService,
                           AuditLogService auditLogService,
                           ClientIpResolver clientIpResolver) {
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public ResponseEntity<List<UserController.UserProfileResponse>> listUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {

        List<UserController.UserProfileResponse> users = userService.listUsers(status, role).stream()
                .map(UserController.UserProfileResponse::new)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserController.UserProfileResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(new UserController.UserProfileResponse(userService.getUserById(id)));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserController.UserProfileResponse> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolesRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User updated = userService.updateRoles(id, request.roles());
        auditLogService.record(actorId(userDetails), AuditEventType.ROLES_UPDATED,
                "Roles updated for user " + id + ": " + request.roles(),
                clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(new UserController.UserProfileResponse(updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserController.UserProfileResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {

        User updated = userService.updateStatus(id, request.status());
        auditLogService.record(actorId(userDetails), AuditEventType.USER_STATUS_UPDATED,
                "Status updated for user " + id + " to " + request.status(),
                clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(new UserController.UserProfileResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        userService.softDelete(id);
        auditLogService.record(actorId(userDetails), AuditEventType.USER_DELETED,
                "User soft deleted: " + id,
                clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    /** The ID of the authenticated admin performing the action. */
    private Long actorId(UserDetails userDetails) {
        return ((AppUser) userDetails).getUser().getId();
    }

    public record UpdateRolesRequest(
            @NotBlank(message = "Roles are required (comma-separated, e.g. CUSTOMER,STORE_OWNER)")
            String roles
    ) {}

    public record UpdateStatusRequest(
            @NotNull(message = "Status is required")
            User.UserStatus status
    ) {}
}
