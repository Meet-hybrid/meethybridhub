package com.meethybridhub.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserDetailsServiceImpl} with a mocked
 * {@link UserRepository} — no Spring context.
 *
 * Focuses on {@code loadUserForAuthentication}'s business validation
 * (email verified + account ACTIVE) and the plain {@code loadUserByUsername}
 * mapping used by Spring Security's DaoAuthenticationProvider.
 */
class UserDetailsServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);

    private User activeVerifiedUser(String email) {
        User user = new User(email, "hash", "Test User");
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return user;
    }

    @Test
    void loadUserForAuthenticationReturnsDetailsForActiveVerifiedUser() {
        User user = activeVerifiedUser("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserForAuthentication("alice@example.com");

        assertThat(details).isInstanceOf(AppUser.class);
        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
        assertThat(((AppUser) details).getPasswordVersion()).isZero();
    }

    @Test
    void loadUserForAuthenticationRejectsUnverifiedEmail() {
        User user = new User("bob@example.com", "hash", "Bob");
        user.setStatus(User.UserStatus.ACTIVE); // status fine, but email never verified
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserForAuthentication("bob@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Email not verified")
                .hasMessageContaining("bob@example.com");
    }

    @Test
    void loadUserForAuthenticationRejectsNonActiveAccount() {
        User user = activeVerifiedUser("carol@example.com");
        user.setStatus(User.UserStatus.SUSPENDED);
        when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserForAuthentication("carol@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Account is not active")
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    void loadUserForAuthenticationThrowsWhenEmailUnknown() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserForAuthentication("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found")
                .hasMessageContaining("ghost@example.com");
    }

    @Test
    void loadUserByUsernameReturnsDetailsForExistingUser() {
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(activeVerifiedUser("alice@example.com")));

        UserDetails details = service.loadUserByUsername("alice@example.com");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsernameThrowsWhenEmailUnknown() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
