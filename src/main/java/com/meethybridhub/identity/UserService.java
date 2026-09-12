package com.meethybridhub.identity;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;


@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    public User register(AuthController.RegisterRequest request) {

    public User register(AuthController.RegisterRequest request) {

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        validatePassword(request.password());


        validatePassword(request.password());


        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim()
        );

        user.setRoles("CUSTOMER");
        user.setStatus(User.UserStatus.PENDING);

        User savedUser = userRepository.save(user);


        user.setRoles("CUSTOMER");
        user.setStatus(User.UserStatus.PENDING);


        User savedUser = userRepository.save(user);


        EmailVerificationToken verificationToken = new EmailVerificationToken(
                savedUser.getId(),
                generateOpaqueToken(),
                Instant.now().plus(VERIFICATION_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS)
        );
        emailVerificationTokenRepository.save(verificationToken);


        try {
            emailService.sendVerificationEmail(
                    savedUser.getEmail(),
                    savedUser.getFullName(),
                    verificationToken.getToken());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", savedUser.getEmail(), e.getMessage());
        }

        log.info("User registered: {} (ID: {}), verification email queued",
                savedUser.getEmail(), savedUser.getId());
        auditLogService.record(savedUser.getId(), AuditEventType.REGISTER,
                "User registered: " + savedUser.getEmail(), null, null);

        return savedUser;
    }


    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new BadRequestException("Verification token has already been used");
        }
        if (verificationToken.isExpired()) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for verification token"));

        user.setEmailVerified(true);
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);


        verificationToken.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(verificationToken);

        auditLogService.record(user.getId(), AuditEventType.EMAIL_VERIFIED,
                "Email verified for user: " + user.getEmail(), null, null);
        log.info("Email verified for user: {}", user.getEmail());
    }


    public void resendVerificationEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {

            log.debug("Verification resend requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();
        if (user.isEmailVerified()) {
            log.debug("Verification resend skipped — email already verified: {}", email);
            return;
        }


        emailVerificationTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId());

        EmailVerificationToken verificationToken = new EmailVerificationToken(
                user.getId(),
                generateOpaqueToken(),
                Instant.now().plus(VERIFICATION_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS)
        );
        emailVerificationTokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken.getToken());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }


        log.info("Verification email resent to user id: {}", user.getId());
    }


    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {

            log.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();


        passwordResetTokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken(
                user.getId(),
                generateOpaqueToken(),
                Instant.now().plus(RESET_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES)
        );
        passwordResetTokenRepository.save(resetToken);


        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken.getToken());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }


        log.info("Password reset token issued for user id: {}", user.getId());
    }


    public void confirmPasswordReset(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Reset token has already been used");
        }
        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        validatePassword(newPassword);

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for reset token"));


        if (user.getStatus() == User.UserStatus.DELETED) {
            throw new BadRequestException("Cannot reset password for a deleted account");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.bumpPasswordVersion();
        userRepository.save(user);


        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        auditLogService.record(user.getId(), AuditEventType.PASSWORD_RESET_CONFIRMED,
                "Password reset confirmed for user: " + user.getEmail(), null, null);
        log.info("Password reset confirmed for user: {}", user.getEmail());
    }


    public void recordLogin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        user.recordLogin();
        userRepository.save(user);

        log.debug("Login recorded for user: {}", email);
    }


    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userDetailsService.loadUserByUsername(email);
    }


    public List<User> listUsers(String status, String role) {
        if (role != null && !role.isBlank()) {
            return userRepository.findByRole(role.trim().toUpperCase(Locale.ROOT));
        }
        if (status != null && !status.isBlank()) {
            try {
                return userRepository.findByStatus(User.UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown status: " + status
                        + ". Valid values: " + Arrays.toString(User.UserStatus.values()));
            }
        }
        return userRepository.findAll();
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }


    public User updateProfile(Long userId, String fullName) {
        User user = getUserById(userId);
        user.setFullName(fullName.trim());
        return userRepository.save(user);
    }


    public void verifyPassword(Long userId, String password) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);


    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);


        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validatePassword(newPassword);


        validatePassword(newPassword);


        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.bumpPasswordVersion();
        userRepository.save(user);

        auditLogService.record(userId, AuditEventType.PASSWORD_CHANGED,
                "Password changed for user ID: " + userId, null, null);
        log.info("Password changed for user ID: {}", userId);
    }


    public User updateRoles(Long userId, String roles) {
        User user = getUserById(userId);
        user.setRoles(normalizeRoles(roles));
        return userRepository.save(user);
    }


    private String normalizeRoles(String roles) {
        Set<String> valid = new LinkedHashSet<>();
        for (String raw : roles.split(",")) {
            String role = raw.trim().toUpperCase(Locale.ROOT);
            if (role.isEmpty()) {
                continue;
            }
            if (!Role.isValid(role)) {
                throw new BadRequestException("Unknown role: " + role
                        + ". Valid roles: " + Arrays.toString(Role.values()));
            }
            valid.add(role);
        }

        if (valid.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }
        return String.join(",", valid);
    }


    public User updateStatus(Long userId, User.UserStatus status) {
        User user = getUserById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }


    public void softDelete(Long userId) {
        User user = getUserById(userId);
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
        log.info("User soft deleted: ID {}", userId);
    }


    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }


        if (password.equals(password.toLowerCase()) ||
            password.equals(password.toUpperCase())) {
            throw new IllegalArgumentException("Password must contain both uppercase and lowercase letters");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }


        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }


        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }


    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
