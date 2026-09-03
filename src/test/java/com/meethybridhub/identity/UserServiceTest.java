package com.meethybridhub.identity;

import com.meethybridhub.common.exception.BadRequestException;
import com.meethybridhub.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link UserService} — no Spring context.
 *
 * Covers the gaps identified in coverage-gaps.md: register, updateProfile,
 * listUsers, resendVerificationEmail, requestPasswordReset,
 * confirmPasswordReset, changePassword, and normalizeRoles.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailService emailService;
    @Mock private AuditLogService auditLogService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, passwordEncoder, userDetailsService,
                emailVerificationTokenRepository, passwordResetTokenRepository,
                emailService, auditLogService);
    }

    // ── register ─────────────────────────────────────────────────

    @Test
    void registerCreatesUserWithVerificationToken() {
        var request = new AuthController.RegisterRequest("alice@example.com", "Password1!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-hash");
        User savedUser = new User("alice@example.com", "encoded-hash", "Alice");
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(request);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).save(any(User.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(auditLogService).record(eq(1L), eq(AuditEventType.REGISTER), anyString(), isNull(), isNull());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        var request = new AuthController.RegisterRequest("alice@example.com", "Password1!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void registerDoesNotRollbackOnEmailFailure() {
        var request = new AuthController.RegisterRequest("alice@example.com", "Password1!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-hash");
        User savedUser = new User("alice@example.com", "encoded-hash", "Alice");
        ReflectionTestUtils.setField(savedUser, "id", 1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendVerificationEmail(anyString(), anyString(), anyString());

        User result = userService.register(request);

        // Registration succeeds even though email failed
        assertThat(result).isNotNull();
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
    }

    // ── updateProfile ────────────────────────────────────────────

    @Test
    void updateProfileChangesFullName() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile(1L, "Alice Updated");

        assertThat(result.getFullName()).isEqualTo("Alice Updated");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileTrimsWhitespace() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile(1L, "  Alice Updated  ");

        assertThat(result.getFullName()).isEqualTo("Alice Updated");
    }

    @Test
    void updateProfileThrowsWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(999L, "New Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ── listUsers ────────────────────────────────────────────────

    @Test
    void listUsersReturnsAllWhenNoFilters() {
        User user = new User("a@b.com", "hash", "A");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.listUsers(null, null);

        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    void listUsersFiltersByStatus() {
        User user = new User("a@b.com", "hash", "A");
        when(userRepository.findByStatus(User.UserStatus.ACTIVE)).thenReturn(List.of(user));

        List<User> result = userService.listUsers("ACTIVE", null);

        assertThat(result).hasSize(1);
        verify(userRepository).findByStatus(User.UserStatus.ACTIVE);
    }

    @Test
    void listUsersFiltersByRole() {
        User user = new User("a@b.com", "hash", "A");
        when(userRepository.findByRole("STORE_OWNER")).thenReturn(List.of(user));

        List<User> result = userService.listUsers(null, "STORE_OWNER");

        assertThat(result).hasSize(1);
        verify(userRepository).findByRole("STORE_OWNER");
    }

    @Test
    void listUsersThrowsOnInvalidStatus() {
        assertThatThrownBy(() -> userService.listUsers("BOGUS", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown status");
    }

    // ── resendVerificationEmail ──────────────────────────────────

    @Test
    void resendVerificationEmailSilentlyIgnoresUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Should not throw
        userService.resendVerificationEmail("unknown@example.com");

        verify(emailVerificationTokenRepository, never()).deleteByUserIdAndUsedAtIsNull(anyLong());
    }

    @Test
    void resendVerificationEmailSkipsAlreadyVerifiedUser() {
        User user = new User("alice@example.com", "hash", "Alice");
        user.setEmailVerified(true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        userService.resendVerificationEmail("alice@example.com");

        verify(emailVerificationTokenRepository, never()).deleteByUserIdAndUsedAtIsNull(anyLong());
    }

    @Test
    void resendVerificationEmailIssuesNewToken() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmailVerified(false);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        userService.resendVerificationEmail("alice@example.com");

        verify(emailVerificationTokenRepository).deleteByUserIdAndUsedAtIsNull(1L);
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
    }

    // ── requestPasswordReset ─────────────────────────────────────

    @Test
    void requestPasswordResetSilentlyIgnoresUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        userService.requestPasswordReset("unknown@example.com");

        verify(passwordResetTokenRepository, never()).deleteByUserIdAndUsedAtIsNull(anyLong());
    }

    @Test
    void requestPasswordResetIssuesTokenForKnownUser() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        userService.requestPasswordReset("alice@example.com");

        verify(passwordResetTokenRepository).deleteByUserIdAndUsedAtIsNull(1L);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    // ── confirmPasswordReset ─────────────────────────────────────

    @Test
    void confirmPasswordResetUpdatesPasswordAndConsumesToken() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setStatus(User.UserStatus.ACTIVE);

        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.isUsed()).thenReturn(false);
        when(token.isExpired()).thenReturn(false);
        when(token.getUserId()).thenReturn(1L);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-hash");

        userService.confirmPasswordReset("valid-token", "NewPassword1!");

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(auditLogService).record(eq(1L), eq(AuditEventType.PASSWORD_RESET_CONFIRMED), anyString(), isNull(), isNull());
    }

    @Test
    void confirmPasswordResetRejectsUsedToken() {
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.isUsed()).thenReturn(true);
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.confirmPasswordReset("used-token", "NewPassword1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void confirmPasswordResetRejectsExpiredToken() {
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.isUsed()).thenReturn(false);
        when(token.isExpired()).thenReturn(true);
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> userService.confirmPasswordReset("expired-token", "NewPassword1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void confirmPasswordResetRejectsDeletedAccount() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setStatus(User.UserStatus.DELETED);

        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.isUsed()).thenReturn(false);
        when(token.isExpired()).thenReturn(false);
        when(token.getUserId()).thenReturn(1L);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.confirmPasswordReset("valid-token", "NewPassword1!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deleted account");
    }

    @Test
    void confirmPasswordResetRejectsWeakPassword() {
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.isUsed()).thenReturn(false);
        when(token.isExpired()).thenReturn(false);
        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        // validatePassword is called BEFORE finding the user, so weak password throws first
        assertThatThrownBy(() -> userService.confirmPasswordReset("valid-token", "weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    // ── changePassword ───────────────────────────────────────────

    @Test
    void changePasswordUpdatesHashAndBumpsVersion() {
        User user = new User("alice@example.com", "old-hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword1!", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword2!")).thenReturn("new-hash");

        userService.changePassword(1L, "OldPassword1!", "NewPassword2!");

        verify(userRepository).save(user);
        verify(auditLogService).record(eq(1L), eq(AuditEventType.PASSWORD_CHANGED), anyString(), isNull(), isNull());
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = new User("alice@example.com", "old-hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1!", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "WrongPassword1!", "NewPassword2!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");
    }

    // ── validatePassword (private, tested through register) ──────

    @Test
    void registerRejectsShortPassword() {
        var request = new AuthController.RegisterRequest("alice@example.com", "Ab1!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void registerRejectsNoDigitPassword() {
        var request = new AuthController.RegisterRequest("alice@example.com", "Abcdefg!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one digit");
    }

    @Test
    void registerRejectsNoSpecialCharPassword() {
        var request = new AuthController.RegisterRequest("alice@example.com", "Abcdefg1", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one special character");
    }

    @Test
    void registerRejectsAllLowercasePassword() {
        var request = new AuthController.RegisterRequest("alice@example.com", "abcdefg1!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase and lowercase");
    }

    @Test
    void registerRejectsAllUppercasePassword() {
        var request = new AuthController.RegisterRequest("alice@example.com", "ABCDEFG1!", "Alice");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase and lowercase");
    }

    // ── normalizeRoles (tested through updateRoles) ──────────────

    @Test
    void updateRolesNormalizesAndDeduplicates() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateRoles(1L, "customer, store_owner, CUSTOMER");

        assertThat(result.getRoles()).isEqualTo("CUSTOMER,STORE_OWNER");
    }

    @Test
    void updateRolesRejectsInvalidRole() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateRoles(1L, "INVALID_ROLE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown role");
    }

    @Test
    void updateRolesRejectsEmptyRoles() {
        User user = new User("alice@example.com", "hash", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateRoles(1L, " , , "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one role");
    }
}
