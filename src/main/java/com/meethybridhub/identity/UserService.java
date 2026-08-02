package com.meethybridhub.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service layer for user management and authentication business logic.
 *
 * This service handles:
 *   - User registration with validation
 *   - Email verification workflow
 *   - Password reset functionality
 *   - Account status management
 *   - Security audit logging
 *
 * Business rules belong here, not in controllers or repositories.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Register a new user with email verification.
     */
    public User register(AuthController.RegisterRequest request) {
        // Validate email uniqueness (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        // Validate password strength
        validatePassword(request.password());

        // Create user entity
        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim()
        );

        // Set initial role (default is CUSTOMER, can be overridden later)
        user.setRoles("CUSTOMER");
        user.setStatus(User.UserStatus.PENDING); // Requires email verification

        // Save user
        User savedUser = userRepository.save(user);
        
        log.info("User registered: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
        
        // TODO: Send email verification (will be implemented in EmailService)
        // generateAndSendVerificationEmail(savedUser);
        
        return savedUser;
    }

    /**
     * Verify user's email with token.
     */
    public void verifyEmail(String token) {
        // TODO: Implement token validation with EmailVerificationTokenRepository
        // EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
        //     .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));
        
        // User user = verificationToken.getUser();
        // user.setEmailVerified(true);
        // user.setStatus(User.UserStatus.ACTIVE);
        // userRepository.save(user);
        
        // emailVerificationTokenRepository.delete(verificationToken);
        
        log.info("Email verification requested for token: {}", token);
        throw new UnsupportedOperationException("Email verification not yet implemented");
    }

    /**
     * Request password reset for a user.
     */
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // TODO: Generate password reset token and send email
        // String resetToken = generateResetToken(user);
        // sendPasswordResetEmail(user, resetToken);
        
        log.info("Password reset requested for user: {}", email);
        
        // For now, just log the request
        throw new UnsupportedOperationException("Password reset not yet implemented");
    }

    /**
     * Confirm password reset with token.
     */
    public void confirmPasswordReset(String token, String newPassword) {
        // TODO: Validate reset token and update password
        // PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
        //     .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));
        
        // Validate token expiration
        // if (resetToken.getExpiresAt().isBefore(Instant.now())) {
        //     throw new IllegalArgumentException("Reset token expired");
        // }
        
        // User user = resetToken.getUser();
        // user.setPasswordHash(passwordEncoder.encode(newPassword));
        // userRepository.save(user);
        
        // passwordResetTokenRepository.delete(resetToken);
        
        log.info("Password reset confirmation requested with token: {}", token);
        throw new UnsupportedOperationException("Password reset confirmation not yet implemented");
    }

    /**
     * Record user login timestamp.
     */
    public void recordLogin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        
        user.recordLogin();
        userRepository.save(user);
        
        log.debug("Login recorded for user: {}", email);
    }

    /**
     * Load user by username (email).
     */
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userDetailsService.loadUserByUsername(email);
    }

    /**
     * Get user by ID.
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    /**
     * Get user by email.
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    /**
     * Update user profile.
     */
    public User updateProfile(Long userId, String fullName) {
        User user = getUserById(userId);
        user.setFullName(fullName.trim());
        return userRepository.save(user);
    }

    /**
     * Change user password.
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Validate new password
        validatePassword(newPassword);
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password changed for user ID: {}", userId);
    }

    /**
     * Update user roles (admin only).
     */
    public User updateRoles(Long userId, String roles) {
        User user = getUserById(userId);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    /**
     * Update user status (admin only).
     */
    public User updateStatus(Long userId, User.UserStatus status) {
        User user = getUserById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    /**
     * Soft delete user (admin only).
     */
    public void softDelete(Long userId) {
        userRepository.softDelete(userId);
        log.info("User soft deleted: ID {}", userId);
    }

    /**
     * Validate password meets security requirements.
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        
        // Check for common password patterns (basic check)
        if (password.equals(password.toLowerCase()) || 
            password.equals(password.toUpperCase())) {
            throw new IllegalArgumentException("Password must contain both uppercase and lowercase letters");
        }
        
        // Check for digits
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        
        // Check for special characters
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }

    /**
     * Generate a secure random token.
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}