package com.meethybridhub.identity;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(new UserProfileResponse(user));
    }


    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        User updatedUser = userService.updateProfile(user.getId(), request.fullName());

        return ResponseEntity.ok(new UserProfileResponse(updatedUser));
    }


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


    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request) {

        User user = userService.getUserByEmail(userDetails.getUsername());


        userService.verifyPassword(user.getId(), request.password());
        userService.softDelete(user.getId());

        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }


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
