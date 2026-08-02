package com.meethybridhub.identity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation that loads users from our database.
 *
 * This bridges Spring Security's authentication mechanism with our User entity.
 * It's responsible for:
 *   - Loading user by username (email)
 *   - Converting our role string format to Spring Security GrantedAuthority objects
 *   - Checking account status before allowing authentication
 *
 * Transactional annotation ensures the database operations are wrapped in a transaction.
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(this::buildUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Convert our User entity to Spring Security's UserDetails.
     */
    private UserDetails buildUserDetails(User user) {
        // Extract roles from comma-separated string
        List<SimpleGrantedAuthority> authorities = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .map(role -> "ROLE_" + role) // Spring Security expects ROLE_ prefix
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Build UserDetails with account status checks
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == User.UserStatus.SUSPENDED)
                .credentialsExpired(false)
                .disabled(!user.canAuthenticate()) // Checks email verification and active status
                .build();
    }

    /**
     * Load user by email with additional business validation.
     * Throws specific exceptions for different failure reasons.
     */
    public UserDetails loadUserForAuthentication(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Business validation before allowing authentication
        if (!user.isEmailVerified()) {
            throw new UsernameNotFoundException("Email not verified for user: " + email);
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("Account is not active: " + user.getStatus());
        }

        return buildUserDetails(user);
    }
}