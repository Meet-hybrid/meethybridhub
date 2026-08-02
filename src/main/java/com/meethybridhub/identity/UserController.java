package com.meethybridhub.identity;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User profile controller for authenticated users.
 *
 * Endpoints:
 *   GET    /api/v1/users/me                - Get current user profile
 *   PUT    /api/v1/users/me                - Update current user profile
 *   POST   /api/v1/users/me/change-password - Change password
 *   DELETE /api/v1/users/me                - Soft delete account
 *
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(new UserProfileResponse(user));
    }

    /**
     * Update current user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        User user = userService.getUserByEmail(userDetails.getUsername());
        User updatedUser = userService.updateProfile(user.getId(), request.fullName());
        
        return ResponseEntity.ok(new UserProfileResponse(updatedUser));
    }

    /**
     * Change current user's password.
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        User user = userService.getUserByEmail(userDetails.getUsername());
        userService.changePassword(
                user.getId(), 
                request.currentPassword(), 
                request.newPassword()
        );
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Soft delete current user's account.
     * This doesn't permanently delete data (for compliance).
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request) {
        
        User user = userService.getUserByEmail(userDetails.getUsername());
        
        // Verify password before deletion
        userService.changePassword(
                user.getId(), 
                request.password(), 
                request.password() // Dummy call to verify password
        );
        
        userService.softDelete(user.getId());
        
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    // Helper method to get user by email
    private User getUserByEmail(String email) {
        // This would be in UserService, but adding here for completeness
        // In reality, UserService should have this method
        throw new UnsupportedOperationException("Implement in UserService");
    }

    // Request/Response records

    public record UpdateProfileRequest(
            @jakarta.validation.constraints.NotBlank(message = "Full name is required")
            @jakarta.validation.constraints.Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
            String fullName
    ) {}

    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank(message = "Current password is required")
            String currentPassword,
            
            @com.meethybridhub.identity.validation.ValidPassword
            String newPassword
    ) {}

    public record DeleteAccountRequest(
            @jakarta.validation.constraints.NotBlank(message = "Password is required")
            String password
    ) {}

    public record UserProfileResponse(
            Long id,
            String email,
            String fullName,
            String roles,
            User.UserStatus status,
            boolean emailVerified,
            String createdAt,
            String updatedAt
    ) {
        public UserProfileResponse(User user) {
            this(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRoles(),
                    user.getStatus(),
                    user.isEmailVerified(),
                    user.getCreatedAt().toString(),
                    user.getUpdatedAt().toString()
            );
        }
    }
}